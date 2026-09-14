package com.example.client_service.grpc;

import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.ValidateRequest;
import com.example.auth.grpc.ValidateResponse;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthGrpcClientTest {

    private static final String TOKEN = "header.payload.signature";
    private static final long DECLARED_TIMEOUT_MS = 5_000L;

    private final FakeAuthService fakeAuthService = new FakeAuthService();

    private Server server;
    private AuthGrpcClient authGrpcClient;

    @BeforeEach
    void startAuthService() throws IOException {
        server = ServerBuilder.forPort(0).addService(fakeAuthService).build().start();
        authGrpcClient = new AuthGrpcClient("localhost", server.getPort());
    }

    @AfterEach
    void stopAuthService() {
        fakeAuthService.release.countDown();
        server.shutdownNow();
    }

    @Test
    void tokenIsForwardedAndTheIdentityComesBack() {
        ValidateResponse response = authGrpcClient.validate(TOKEN);

        assertThat(fakeAuthService.receivedTokens).containsExactly(TOKEN);
        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo("42");
        assertThat(response.getRole()).isEqualTo("CLIENT");
    }

    @Test
    void everyCallCarriesADeadline() {
        authGrpcClient.validate(TOKEN);

        assertThat(fakeAuthService.deadlinesOnArrivalMs)
                .singleElement()
                .satisfies(remaining -> assertThat(remaining)
                        .isPositive()
                        .isLessThanOrEqualTo(DECLARED_TIMEOUT_MS));
    }

    @Test
    void deadlineIsRecomputedPerCallInsteadOfAgeingWithTheStub() throws InterruptedException {
        authGrpcClient.validate(TOKEN);
        Thread.sleep(1_500);
        authGrpcClient.validate(TOKEN);

        assertThat(fakeAuthService.deadlinesOnArrivalMs)
                .hasSize(2)
                .allSatisfy(remaining -> assertThat(remaining).isGreaterThan(DECLARED_TIMEOUT_MS - 1_000));
    }

    @Test
    void unresponsiveAuthServiceFailsWithDeadlineExceededInsteadOfHangingForever() {
        fakeAuthService.neverAnswer = true;

        long startedAt = System.currentTimeMillis();
        assertThatThrownBy(() -> authGrpcClient.validate(TOKEN))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> Status.fromThrowable((StatusRuntimeException) e).getCode())
                .isEqualTo(Status.Code.DEADLINE_EXCEEDED);

        assertThat(System.currentTimeMillis() - startedAt)
                .isBetween(DECLARED_TIMEOUT_MS - 500, DECLARED_TIMEOUT_MS + 5_000);
    }

    @Test
    void failingAuthServiceSurfacesAsUnavailableNotAsAValidResponse() {
        fakeAuthService.unavailable = true;

        assertThatThrownBy(() -> authGrpcClient.validate(TOKEN))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> Status.fromThrowable((StatusRuntimeException) e).getCode())
                .isEqualTo(Status.Code.UNAVAILABLE);
    }

    private static class FakeAuthService extends AuthServiceGrpc.AuthServiceImplBase {

        private final List<String> receivedTokens = new CopyOnWriteArrayList<>();
        private final List<Long> deadlinesOnArrivalMs = new CopyOnWriteArrayList<>();
        private final CountDownLatch release = new CountDownLatch(1);

        private volatile boolean unavailable;
        private volatile boolean neverAnswer;

        @Override
        public void validate(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
            receivedTokens.add(request.getToken());
            Deadline deadline = Context.current().getDeadline();
            deadlinesOnArrivalMs.add(deadline == null ? -1L : deadline.timeRemaining(TimeUnit.MILLISECONDS));

            if (neverAnswer) {
                awaitRelease();
                return;
            }
            if (unavailable) {
                responseObserver.onError(Status.UNAVAILABLE
                        .withDescription("auth-service down")
                        .asRuntimeException());
                return;
            }
            responseObserver.onNext(ValidateResponse.newBuilder()
                    .setValid(true)
                    .setUserId("42")
                    .setRole("CLIENT")
                    .build());
            responseObserver.onCompleted();
        }

        private void awaitRelease() {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
