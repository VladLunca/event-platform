package com.example.client_service.service;

import com.example.client_service.exception.ForbiddenException;
import com.example.client_service.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Service
public class EventVerificationService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public EventVerificationService(@Value("${EVENT_SERVICE_URL:http://event-service:8080}") String eventServiceUrl) {
        this.restClient = RestClient.builder()
                .requestFactory(timeoutBoundRequestFactory())
                .baseUrl(eventServiceUrl)
                .build();
    }

    /**
     * Without explicit timeouts the call blocks forever when the event-service is
     * unreachable without refusing the connection (paused container, dropped packets),
     * and the "service unavailable" branch below is never reached because no exception
     * is ever raised. Both timeouts surface as an exception, which maps to 409.
     */
    private static ClientHttpRequestFactory timeoutBoundRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    public void verifyTicketOwnership(String ticketId, String userId, String authHeader) {
        Map<?, ?> response = fetchTicket(ticketId, authHeader);

        // Kept outside the call above: throwing it inside the try block would make it
        // fall into the generic catch and be reported as "event service unavailable".
        if (response == null || !userId.equals(response.get("ownerUserId"))) {
            throw new ForbiddenException("Biletul nu va apartine");
        }
    }

    private Map<?, ?> fetchTicket(String ticketId, String authHeader) {
        try {
            return restClient.get()
                    .uri("/events/tickets/{ticketId}", ticketId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Biletul nu a fost gasit in sistemul de evenimente");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // The event-service refused the lookup, so the ticket is not the caller's.
            throw new ForbiddenException("Biletul nu va apartine");
        } catch (Exception e) {
            throw new IllegalStateException("Serviciul de evenimente nu este disponibil");
        }
    }
}
