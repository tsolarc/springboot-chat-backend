package com.Backend.User.controller;

import com.Backend.User.entity.MusicGenre;
import com.Backend.User.service.IMusicGenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/genres")
public class GenreController {

    @Autowired
    private IMusicGenreService genreService;

    @GetMapping
    public ResponseEntity<List<MusicGenre>> getAllGenres() {
        return ResponseEntity.ok(genreService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MusicGenre> getGenreById(@PathVariable Long id) {
        return genreService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MusicGenre> createGenre(@RequestBody MusicGenre genre) {
        return ResponseEntity.ok(genreService.create(genre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MusicGenre> updateGenre(@PathVariable Long id, @RequestBody MusicGenre genre) {
        return ResponseEntity.ok(genreService.update(id, genre));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGenre(@PathVariable Long id) {
        genreService.delete(id);
        return ResponseEntity.ok().build();
    }
}
