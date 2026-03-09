package com.Backend.User.service;

import com.Backend.User.dto.UserDTO;
import com.Backend.User.entity.User;
import com.Backend.User.mapper.UserMapper;
import com.Backend.User.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDTO login(String email, String password) {
        User user = userRepository.findByEmailAndPassword(email, password)
                .orElseThrow(() -> new NoSuchElementException("Credenciales inválidas"));
        return userMapper.convertToDto(user);
    }

    @Override
    public UserDTO register(UserDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }

        User user = userMapper.convertToEntity(dto);
        User saved = userRepository.save(user);
        return userMapper.convertToDto(saved);
    }
}
