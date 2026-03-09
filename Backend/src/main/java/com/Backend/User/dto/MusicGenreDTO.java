package com.Backend.User.dto;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicGenreDTO implements Serializable {
    private Long id;
    private String name;
}
