ALTER TABLE messages ADD COLUMN IF NOT EXISTS client_message_id uuid;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS deleted_at timestamp(6);
ALTER TABLE messages ALTER COLUMN message_content TYPE text;
ALTER TABLE message_history ALTER COLUMN previous_content TYPE text;
CREATE UNIQUE INDEX IF NOT EXISTS uq_message_sender_client_id ON messages(sender_id, client_message_id);
CREATE INDEX IF NOT EXISTS idx_message_seek ON messages(chat_id, sended_at DESC, id DESC);

ALTER TABLE chats ADD COLUMN IF NOT EXISTS last_activity_at timestamp(6);
ALTER TABLE chats ADD COLUMN IF NOT EXISTS direct_key varchar(80);
UPDATE chats c SET last_activity_at = greatest(c.created_at, coalesce(
    (SELECT max(m.sended_at) FROM messages m WHERE m.chat_id = c.id), c.created_at))
WHERE c.last_activity_at IS NULL;

DELETE FROM chat_member a USING chat_member b
WHERE a.id > b.id AND a.chat_id = b.chat_id AND a.user_id = b.user_id;
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_member_user ON chat_member(chat_id, user_id);
ALTER TABLE chat_member ADD COLUMN IF NOT EXISTS last_read_message_id bigint;

WITH pairs AS (
    SELECT cm.chat_id, min(cm.user_id) AS first_user, max(cm.user_id) AS second_user
    FROM chat_member cm JOIN chats c ON c.id = cm.chat_id
    WHERE c.chat_type = 'PRIVATE'
    GROUP BY cm.chat_id
    HAVING count(DISTINCT cm.user_id) = 2
), ranked AS (
    SELECT chat_id, concat(first_user, ':', second_user) AS pair_key,
           row_number() OVER (PARTITION BY first_user, second_user ORDER BY chat_id) AS rank
    FROM pairs
)
UPDATE chats c SET direct_key = ranked.pair_key
FROM ranked WHERE c.id = ranked.chat_id AND ranked.rank = 1 AND c.direct_key IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_direct_key ON chats(direct_key) WHERE direct_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_chat_activity ON chats(last_activity_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS message_outbox (
    id uuid PRIMARY KEY,
    routing_key varchar(80) NOT NULL,
    payload text NOT NULL,
    created_at timestamp(6) NOT NULL,
    next_attempt_at timestamp(6) NOT NULL,
    published_at timestamp(6),
    attempts integer NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_message_outbox_pending ON message_outbox(next_attempt_at, created_at)
WHERE published_at IS NULL;
