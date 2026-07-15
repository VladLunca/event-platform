package com.example.event_service.grpc;

import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.ValidateRequest;
import com.example.auth.grpc.ValidateResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthGrpcClient {
    private final AuthServiceGrpc.AuthServiceBlockingStub stub;

    public AuthGrpcClient(@Value("${AUTH_GRPC_HOST:localhost}") String host, @Value("${AUTH_GRPC_PORT:9090}") int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = AuthServiceGrpc.newBlockingStub(channel);
    }

    public ValidateResponse validate(String token) {
        return stub.validate(ValidateRequest.newBuilder().setToken(token).build());
    }
}
