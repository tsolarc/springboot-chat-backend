package com.Backend.User.service;

import com.Backend.User.dto.MusicGenreDTO;
import com.Backend.User.entity.MusicGenre;
import com.Backend.User.repository.MusicGenreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MusicGenreServiceImpl implements IMusicGenreService {

    @Autowired
    private MusicGenreRepository genreRepository;

    @Override
    public List<MusicGenre> findAll() {
        return genreRepository.findAll();
    }

    @Override
    public Optional<MusicGenre> findById(Long id) {
        return genreRepository.findById(id);
    }

    @Override
    public MusicGenre create(MusicGenre genre) {
        return genreRepository.save(genre);
    }

    @Override
    public MusicGenre update(Long id, MusicGenre genreDetails) {
        return genreRepository.findById(id)
                .map(genre -> {
                    genre.setName(genreDetails.getName());
                    return genreRepository.save(genre);
                })
                .orElseThrow(() -> new RuntimeException("Género no encontrado"));
    }

    @Override
    public void delete(Long id) {
        MusicGenre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Género no encontrado"));
        genreRepository.delete(genre);
    }

    @Override
    public MusicGenreDTO findDTOById(Long id) {
        MusicGenre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Género musical no encontrado con id: " + id));

        return new MusicGenreDTO(genre.getId(), genre.getName());
    }
}
