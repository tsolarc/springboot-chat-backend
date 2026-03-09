package com.Backend.User.service;

import com.Backend.User.dto.MusicGenreDTO;
import com.Backend.User.entity.MusicGenre;

import java.util.List;
import java.util.Optional;

public interface IMusicGenreService {
    List<MusicGenre> findAll();
    Optional<MusicGenre> findById(Long id);
    MusicGenre create(MusicGenre genre);
    MusicGenre update(Long id, MusicGenre genreDetails);
    void delete(Long id);
    MusicGenreDTO findDTOById(Long id);
}
