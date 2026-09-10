package com.example.client_service.service;

import com.example.client_service.exception.ForbiddenException;
import com.example.client_service.exception.NotFoundException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventVerificationServiceTest {

    private static final String TICKET_ID = "t1";

    private HttpServer stub;
    private final AtomicReference<String> receivedAuthHeader = new AtomicReference<>();

    @AfterEach
    void stopStub() {
        if (stub != null) {
            stub.stop(0);
        }
    }

    private EventVerificationService eventServiceReturning(int status, String body) throws IOException {
        stub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stub.createContext("/events/tickets/", exchange -> {
            receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        stub.start();
        return new EventVerificationService("http://localhost:" + stub.getAddress().getPort());
    }

    @Test
    void ticketOwnedBySomeoneElseIsForbiddenNotUnavailable() throws Exception {
        EventVerificationService service =
                eventServiceReturning(200, "{\"ownerUserId\":\"other-user\"}");

        assertThatThrownBy(() -> service.verifyTicketOwnership(TICKET_ID, "me", "Bearer x"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Biletul nu va apartine");
    }

    @Test
    void ticketOwnedByCallerIsAccepted() throws Exception {
        EventVerificationService service =
                eventServiceReturning(200, "{\"ownerUserId\":\"me\"}");

        assertThatCode(() -> service.verifyTicketOwnership(TICKET_ID, "me", "Bearer x"))
                .doesNotThrowAnyException();
    }

    @Test
    void authorizationHeaderIsForwardedToEventService() throws Exception {
        EventVerificationService service =
                eventServiceReturning(200, "{\"ownerUserId\":\"me\"}");

        service.verifyTicketOwnership(TICKET_ID, "me", "Bearer my-token");

        assertThat(receivedAuthHeader.get()).isEqualTo("Bearer my-token");
    }

    @Test
    void unknownTicketIsNotFound() throws Exception {
        EventVerificationService service = eventServiceReturning(404, null);

        assertThatThrownBy(() -> service.verifyTicketOwnership(TICKET_ID, "me", "Bearer x"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectionFromEventServiceIsForbidden() throws Exception {
        EventVerificationService service = eventServiceReturning(403, null);

        assertThatThrownBy(() -> service.verifyTicketOwnership(TICKET_ID, "me", "Bearer x"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void expiredTokenFromEventServiceIsForbidden() throws Exception {
        EventVerificationService service = eventServiceReturning(401, null);

        assertThatThrownBy(() -> service.verifyTicketOwnership(TICKET_ID, "me", "Bearer x"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void brokenEventServiceIsReportedAsUnavailable() throws Exception {
        EventVerificationService service = eventServiceReturning(500, null);

        assertThatThrownBy(() -> service.verifyTicketOwnership(TICKET_ID, "me", "Bearer x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nu este disponibil");
    }

    @Test
    void unresponsiveEventServiceDoesNotBlockForever() throws Exception {
        EventVerificationService service = eventServiceThatNeverAnswers();

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> service.verifyTicketOwnership(TICKET_ID, "me", "Bearer x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nu este disponibil");

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(15));
    }

    private EventVerificationService eventServiceThatNeverAnswers() throws IOException {
        stub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stub.createContext("/events/tickets/", exchange -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        stub.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }));
        stub.start();
        return new EventVerificationService("http://localhost:" + stub.getAddress().getPort());
    }
}
