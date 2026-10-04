package com.Backend.Chat.entity.Message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_outbox")
@Getter
@Setter
public class MessageOutboxEvent {

    @Id
    private UUID id;

    @Column(name = "routing_key", nullable = false, length = 80)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "origin_user_id")
    private Long originUserId;

    @Column(name = "origin_chat_id")
    private Long originChatId;

    @Column(name = "origin_message_id")
    private Long originMessageId;

    @Column(name = "origin_client_message_id")
    private UUID originClientMessageId;

    @Column(name = "origin_operation", length = 40)
    private String originOperation;

    @Column(name = "origin_source", length = 100)
    private String originSource;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "claimed_until")
    private Instant claimedUntil;

    @Column(name = "dead_lettered_at")
    private Instant deadLetteredAt;

    @Column(name = "origin_request_id", length = 64)
    private String originRequestId;
}
