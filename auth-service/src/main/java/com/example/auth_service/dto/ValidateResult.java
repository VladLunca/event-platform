package com.example.auth_service.dto;

public record ValidateResult(boolean valid, String userId, String role) {}
