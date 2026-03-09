package com.Backend.User.repository;

import com.Backend.User.entity.MusicGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicGenreRepository extends JpaRepository<MusicGenre, Long> {
}
