package com.Backend.User.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicDTO implements Serializable {
    private Long id;
    private String name;
    private String username;
    private String location;
}
