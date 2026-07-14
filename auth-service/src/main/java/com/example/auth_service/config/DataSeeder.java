package com.example.auth_service.config;

import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    @Bean
    public ApplicationRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@platform.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@platform.com");
                admin.setParola(passwordEncoder.encode("admin123"));
                admin.setRol(Role.ADMIN);
                userRepository.save(admin);
            }
        };
    }
}
