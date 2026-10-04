ALTER TABLE message_outbox ADD COLUMN claim_token uuid;
ALTER TABLE message_outbox ADD COLUMN claimed_until timestamp with time zone;
ALTER TABLE message_outbox ADD COLUMN dead_lettered_at timestamp with time zone;
ALTER TABLE message_outbox ADD COLUMN origin_request_id varchar(64);
ALTER TABLE scheduled_task_event_log ADD COLUMN origin_request_id varchar(64);
CREATE INDEX idx_task_event_request ON scheduled_task_event_log(origin_request_id);
CREATE INDEX idx_task_event_run ON scheduled_task_event_log(task_log_id);
DROP INDEX idx_message_outbox_pending;
CREATE INDEX idx_message_outbox_pending ON message_outbox(next_attempt_at, created_at, id)
    WHERE published_at IS NULL AND dead_lettered_at IS NULL;
CREATE INDEX idx_message_outbox_completed ON message_outbox(published_at) WHERE published_at IS NOT NULL;
