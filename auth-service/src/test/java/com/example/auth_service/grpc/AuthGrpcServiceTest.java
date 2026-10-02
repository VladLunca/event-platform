package com.example.auth_service.grpc;

import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.LoginRequest;
import com.example.auth.grpc.LoginResponse;
import com.example.auth.grpc.LogoutRequest;
import com.example.auth.grpc.LogoutResponse;
import com.example.auth.grpc.ValidateRequest;
import com.example.auth.grpc.ValidateResponse;
import com.example.auth_service.dto.ValidateResult;
import com.example.auth_service.exception.InvalidCredentialsException;
import com.example.auth_service.service.AuthService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthGrpcServiceTest {

    private static final String TOKEN = "header.payload.signature";

    private final AuthService authService = mock(AuthService.class);

    private Server server;
    private ManagedChannel channel;
    private AuthServiceGrpc.AuthServiceBlockingStub stub;

    @BeforeEach
    void startInProcessServer() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new AuthGrpcService(authService))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = AuthServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stopInProcessServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void validTokenIsAnsweredWithTheIdentityBehindIt() {
        when(authService.validate(TOKEN)).thenReturn(new ValidateResult(true, "42", "CLIENT", "client@platform.com"));

        ValidateResponse response = stub.validate(ValidateRequest.newBuilder().setToken(TOKEN).build());

        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo("42");
        assertThat(response.getRole()).isEqualTo("CLIENT");
        assertThat(response.getEmail()).isEqualTo("client@platform.com");
    }

    @Test
    void validTokenWithoutAnEmailAnswersWithAnEmptyEmailInsteadOfFailing() {
        when(authService.validate(TOKEN)).thenReturn(new ValidateResult(true, "42", "CLIENT", null));

        ValidateResponse response = stub.validate(ValidateRequest.newBuilder().setToken(TOKEN).build());

        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo("42");
        assertThat(response.getEmail()).isEmpty();
    }

    @Test
    void rejectedTokenIsAnsweredWithoutAnyIdentity() {
        when(authService.validate(TOKEN)).thenReturn(new ValidateResult(false, null, null, null));

        ValidateResponse response = stub.validate(ValidateRequest.newBuilder().setToken(TOKEN).build());

        assertThat(response.getValid()).isFalse();
        assertThat(response.getUserId()).isEmpty();
        assertThat(response.getRole()).isEmpty();
        assertThat(response.getEmail()).isEmpty();
    }

    @Test
    void rejectedTokenIsAnOkResponseNotAGrpcError() {
        when(authService.validate(anyString())).thenReturn(new ValidateResult(false, null, null, null));

        ValidateResponse response = stub.validate(ValidateRequest.newBuilder().setToken("garbage").build());

        assertThat(response.getValid()).isFalse();
    }

    @Test
    void successfulLoginReturnsTheIssuedToken() {
        when(authService.login("client@platform.com", "secret")).thenReturn(TOKEN);

        LoginResponse response = stub.login(LoginRequest.newBuilder()
                .setUsername("client@platform.com")
                .setPassword("secret")
                .build());

        assertThat(response.getToken()).isEqualTo(TOKEN);
    }

    @Test
    void wrongCredentialsBecomeUnauthenticatedNotUnknown() {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("Credentiale invalide"));

        assertThatThrownBy(() -> stub.login(LoginRequest.newBuilder()
                .setUsername("client@platform.com")
                .setPassword("wrong")
                .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> Status.fromThrowable((StatusRuntimeException) e).getCode())
                .isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    void wrongCredentialsNeverReachTheBlacklist() {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("Credentiale invalide"));

        assertThatThrownBy(() -> stub.login(LoginRequest.newBuilder()
                .setUsername("client@platform.com")
                .setPassword("wrong")
                .build()))
                .isInstanceOf(StatusRuntimeException.class);

        verify(authService, never()).logout(anyString());
    }

    @Test
    void logoutHandsTheTokenToTheServiceAndReportsSuccess() {
        LogoutResponse response = stub.logout(LogoutRequest.newBuilder().setToken(TOKEN).build());

        assertThat(response.getSuccess()).isTrue();
        verify(authService).logout(TOKEN);
    }
}
