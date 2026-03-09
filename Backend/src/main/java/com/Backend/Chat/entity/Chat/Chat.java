package com.Backend.Chat.entity.Chat;

import com.Backend.Chat.Enums.ChatType;
import com.Backend.Chat.entity.Message.Message;
import lombok.*;
import java.util.*;
import jakarta.persistence.*;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chats", indexes = {
        @Index(name = "idx_chat_is_private", columnList = "is_private"),
        @Index(name = "idx_chat_created_at", columnList = "created_at"),
        @Index(name = "idx_chat_type", columnList = "chat_type")
})
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String chatName;

    @Enumerated(EnumType.STRING)
    private ChatType chatType;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMember> members = new ArrayList<>();

    @Column(name = "is_private")
    @Builder.Default
    private boolean isPrivate = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Date();
    }

    public boolean hasUser(Long userId) {
        return members.stream().anyMatch(member -> member.getUser().getId().equals(userId));
    }
}
