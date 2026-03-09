package com.Backend.User.service;

import com.Backend.User.dto.UserDTO;

public interface IAuthService {
    UserDTO login(String email, String password);
    UserDTO register(UserDTO userDTO);
}

