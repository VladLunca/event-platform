package com.example.event_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateUserRequest {
    private String email;
    private String password;
    private String role;
}
