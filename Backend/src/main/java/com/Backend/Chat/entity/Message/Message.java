package com.Backend.Chat.entity.Message;

import com.Backend.Chat.Enums.MessageType;
import com.Backend.Chat.entity.Chat.Chat;
import jakarta.persistence.*;
import lombok.*;
import com.Backend.User.entity.User;

import java.util.*;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_chat_id", columnList = "chat_id"),
        @Index(name = "idx_message_chat_sended_at", columnList = "chat_id, sended_at"),
        @Index(name = "idx_message_sender_id", columnList = "sender_id")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String messageContent;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Temporal(TemporalType.TIMESTAMP)
    private Date sendedAt;

    @ElementCollection
    @CollectionTable(name = "message_read_status", joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyJoinColumn(name = "user_id")
    @Column(name = "read_at")
    @Builder.Default
    private Map<User, Date> readByUsers = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        this.sendedAt = new Date();
    }

    public boolean isReadBy(User user) {
        return readByUsers.containsKey(user);
    }

    public void markAsReadBy(User user) {
        readByUsers.put(user, new Date());
    }
}
