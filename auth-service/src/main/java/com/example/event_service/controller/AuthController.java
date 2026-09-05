package com.example.event_service.controller;

import com.example.event_service.dto.CreateUserRequest;
import com.example.event_service.dto.LoginRequest;
import com.example.event_service.exception.InvalidCredentialsException;
import com.example.event_service.model.User;
import com.example.event_service.service.AuthService;
import com.example.event_service.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok(Map.of("success", true));
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody CreateUserRequest request) {
        try {
            String token = authService.register(request.getEmail(), request.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody CreateUserRequest request) {
        String token = authHeader.replace("Bearer ", "");

        if (!jwtService.isValid(token) || !"ADMIN".equals(jwtService.getRole(token))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acces interzis"));
        }

        try {
            authService.createUser(request.getEmail(), request.getPassword(), request.getRole());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User creat cu succes"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
