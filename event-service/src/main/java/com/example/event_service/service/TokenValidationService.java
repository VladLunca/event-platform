package com.example.event_service.service;

import com.example.auth.grpc.ValidateResponse;
import com.example.event_service.exception.ForbiddenException;
import com.example.event_service.exception.UnauthorizedException;
import com.example.event_service.grpc.AuthGrpcClient;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TokenValidationService {
    private final AuthGrpcClient authGrpcClient;

    @Autowired
    public TokenValidationService(AuthGrpcClient authGrpcClient) {
        this.authGrpcClient = authGrpcClient;
    }

    public ValidateResponse requireAuth( String authHeader){
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            throw new UnauthorizedException("Token lipsa");
        }
        ValidateResponse response = authGrpcClient.validate(authHeader.substring(7));
        if (!response.getValid()) {
            throw new UnauthorizedException("Token invalid sau expirat");
        }
        return response;
    }
    public ValidateResponse requireRole(String authHeader, String... roles) {
        ValidateResponse response = requireAuth(authHeader);
        for (String role : roles) {
            if (role.equals(response.getRole())) return response;
        }
        throw new ForbiddenException("Nu aveti permisiunile necesare");
    }
    public Optional<ValidateResponse> validateSilently(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            ValidateResponse response = requireAuth(authHeader);
            return Optional.of(response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
