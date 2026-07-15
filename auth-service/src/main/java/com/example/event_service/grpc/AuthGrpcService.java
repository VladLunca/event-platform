package com.example.event_service.grpc;

import com.example.auth.grpc.*;
import com.example.event_service.dto.ValidateResult;
import com.example.event_service.exception.InvalidCredentialsException;
import com.example.event_service.service.AuthService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    public AuthGrpcService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());
            responseObserver.onNext(LoginResponse.newBuilder().setToken(token).build());
            responseObserver.onCompleted();
        } catch (InvalidCredentialsException e) {
            responseObserver.onError(
                Status.UNAUTHENTICATED.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void validate(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
        ValidateResult result = authService.validate(request.getToken());
        ValidateResponse.Builder builder = ValidateResponse.newBuilder().setValid(result.valid());
        if (result.valid()) {
            builder.setUserId(result.userId()).setRole(result.role());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        authService.logout(request.getToken());
        responseObserver.onNext(LogoutResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }
}
