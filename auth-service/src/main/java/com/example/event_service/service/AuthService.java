package com.example.event_service.service;

import com.example.event_service.dto.ValidateResult;
import com.example.event_service.exception.InvalidCredentialsException;
import com.example.event_service.model.Role;
import com.example.event_service.model.User;
import com.example.event_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       TokenBlacklistService blacklistService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.passwordEncoder = passwordEncoder;
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Credentiale invalide"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Credentiale invalide");
        }

        return jwtService.generate(user.getId(), user.getRole().name());
    }

    public ValidateResult validate(String token) {
        if (blacklistService.isBlacklisted(token) || !jwtService.isValid(token)) {
            return new ValidateResult(false, null, null);
        }
        return new ValidateResult(true, jwtService.getUserId(token), jwtService.getRole(token));
    }

    public void logout(String token) {
        blacklistService.add(token);
    }

    public void createUser(String email, String password, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email deja existent");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.valueOf(role));
        userRepository.save(user);
    }
}
