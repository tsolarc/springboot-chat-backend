package com.Backend.Security.Controller;

import com.Backend.Security.DTO.AuthRequestDTO;
import com.Backend.Security.Service.JWTService;
import com.Backend.User.Principal.UserPrincipal;
import com.Backend.User.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JWTService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of("token", token));
    }
}
