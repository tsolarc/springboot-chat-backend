package com.Backend.User.service;

import java.util.List;
import java.util.Optional;
import com.Backend.User.dto.UserDTO;

public interface IUserService {
    List<UserDTO> getAllUsers();
    Optional<UserDTO> getUserById(Long id);
    UserDTO saveUser(UserDTO userDTO);
    void deleteUser(Long id);
    UserDTO updateUser(Long id, UserDTO userDTO);
    UserDTO findByUsername(String username);
}
