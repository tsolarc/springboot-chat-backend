package com.Backend.User.dto;

import java.io.Serializable;
import lombok.*;

@Data
@AllArgsConstructor
public class ChatMemberDTO implements Serializable {
    private long UserID;
    private String username;
}