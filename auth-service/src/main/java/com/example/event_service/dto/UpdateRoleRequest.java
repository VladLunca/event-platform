package com.example.event_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateRoleRequest {
    private String email;
    private String role;
}
