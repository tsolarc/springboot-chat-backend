package com.Backend.User.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Backend.User.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndPassword(String email, String password);
    Optional<User> findUserById(Long UserId);
    Optional<User> findByUsername(String username);
}
