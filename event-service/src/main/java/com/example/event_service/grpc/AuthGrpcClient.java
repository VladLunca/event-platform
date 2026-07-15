package com.example.event_service.grpc;

import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.ValidateRequest;
import com.example.auth.grpc.ValidateResponse;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthGrpcClient {
    private final AuthServiceGrpc.AuthServiceBlockingStub stub;

    public AuthGrpcClient(GrpcChannelFactory channels) {
        this.stub = AuthServiceGrpc.newBlockingStub(channels.createChannel("auth-service"));
    }

    public ValidateResponse validate(String token) {
        return stub.validate(ValidateRequest.newBuilder().setToken(token).build());
    }
}
