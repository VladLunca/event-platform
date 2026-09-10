package com.example.client_service.grpc;

import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.ValidateRequest;
import com.example.auth.grpc.ValidateResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AuthGrpcClient {

    private static final long VALIDATE_TIMEOUT_SECONDS = 5;

    private final AuthServiceGrpc.AuthServiceBlockingStub stub;

    public AuthGrpcClient(@Value("${AUTH_GRPC_HOST:localhost}") String host, @Value("${AUTH_GRPC_PORT:9090}") int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = AuthServiceGrpc.newBlockingStub(channel);
    }

    /**
     * The deadline is applied per call, never on the stored stub: a gRPC deadline is an
     * absolute point in time, so a stub built once with withDeadlineAfter would stop
     * working entirely a few seconds after startup. Without any deadline the blocking
     * stub waits forever when auth-service is unreachable but not refusing connections.
     */
    public ValidateResponse validate(String token) {
        return stub.withDeadlineAfter(VALIDATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .validate(ValidateRequest.newBuilder().setToken(token).build());
    }
}
