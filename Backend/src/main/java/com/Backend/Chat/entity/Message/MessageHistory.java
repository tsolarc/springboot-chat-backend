package com.Backend.Chat.entity.Message;

import com.Backend.Chat.Enums.MessageType;
import com.Backend.User.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "message_history")
@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name= "message_id")
    private Message message;

    private String previousContent;

    @Enumerated(EnumType.STRING)
    private MessageType previousType;

    @Temporal(TemporalType.TIMESTAMP)
    private Date editedAt;

    @ManyToOne
    @JoinColumn(name= "editor_id")
    private User editor;
}
