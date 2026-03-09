package com.Backend.User.service;

import com.Backend.User.dto.UserDTO;
import com.Backend.User.entity.User;
import com.Backend.User.mapper.UserMapper;
import com.Backend.User.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Cacheable("allUsers")
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable("userById")
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id).map(userMapper::convertToDto);
    }

    @Override
    @Transactional
    public UserDTO saveUser(UserDTO userDTO) {
        User user = userMapper.convertToEntity(userDTO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.info("User created successfully");
        return userMapper.convertToDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        log.info("User's information has been deleted successfully");
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        boolean passwordIsValid = validateUserPassword(userDTO.getPassword());

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));

        if (passwordIsValid) {
            userDTO.setPassword(setPasswordEncoder(userDTO.getPassword()));
        }
        userMapper.updateEntity(userDTO, existingUser);
        User updatedUser = userRepository.save(existingUser);
        log.info("The user's {} data has been successfully updated", updatedUser.getUsername());
        return userMapper.convertToDto(updatedUser);
    }

    @Override
    @Cacheable("userByUsername")
    public UserDTO findByUsername(String username){
        return userRepository.findByUsername(username)
                .map(userMapper::convertToDto)
                .orElseThrow(() -> new NoSuchElementException("User not found with username: " + username));
    }

    private boolean validateUserPassword(String password) {
        return password != null && !password.isEmpty();
    }

    private String setPasswordEncoder(String password) {
        return passwordEncoder.encode(password);
    }
}
