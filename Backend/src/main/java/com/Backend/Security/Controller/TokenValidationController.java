package com.Backend.Security.Controller;

import com.Backend.Security.Service.JWTService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class TokenValidationController {

    private final JWTService jwtService;

    public TokenValidationController(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        boolean isValid = jwtService.isTokenValid(token);

        if (isValid) {
            String username = jwtService.extractUsername(token);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("username", username);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
