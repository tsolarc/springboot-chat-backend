package com.Backend.User.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private String location;
    private String biography;
    private List<String> socialNetworks;
    private Set<MusicGenreDTO> musicGenres;
    private String profilePhotoUrl;
    private boolean ProfileComplete;
    @Transient
    private List<Long> musicGenresIds;
}
