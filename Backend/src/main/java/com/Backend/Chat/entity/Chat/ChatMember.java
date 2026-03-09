package com.Backend.Chat.entity.Chat;

import com.Backend.User.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chat_member", indexes = {
        @Index(name = "idx_chat_member_chat_id", columnList = "chat_id"),
        @Index(name = "idx_chat_member_user_id", columnList = "user_id"),
        @Index(name = "idx_chat_member_composite", columnList = "chat_id, user_id")
})
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "joined_at")
    private Date joinedAt;
}
