package com.Backend.User.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.dto.UserBasicDTO;
import com.Backend.User.service.IUserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    public record CreateUserRequest(@NotBlank String name, @NotBlank String username,
                                    @NotBlank @Email String email, @NotBlank @Size(min = 8) String password) {}

    @PostMapping("/create")
    public UserBasicDTO createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO user = new UserDTO();
        user.setName(request.name());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(request.password());
        return toPublic(userService.saveUser(user));
    }

    @GetMapping("/me")
    public UserBasicDTO me(Principal principal) {
        return toPublic(userService.findByUsername(principal.getName()));
    }

    @GetMapping("/{id}")
    public UserBasicDTO getUserById(@PathVariable Long id) {
        return toPublic(userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id)));
    }

    private UserBasicDTO toPublic(UserDTO user) {
        return new UserBasicDTO(user.getId(), user.getName(), user.getUsername(), user.getLocation());
    }
}
