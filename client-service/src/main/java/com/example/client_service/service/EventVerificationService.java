package com.example.client_service.service;

import com.example.client_service.exception.ForbiddenException;
import com.example.client_service.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class EventVerificationService {

    private final RestClient restClient;

    public EventVerificationService(@Value("${EVENT_SERVICE_URL:http://event-service:8080}") String eventServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(eventServiceUrl)
                .build();
    }

    public void verifyTicketOwnership(String ticketId, String userId) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/events/tickets/{ticketId}", ticketId)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !userId.equals(response.get("ownerUserId"))) {
                throw new ForbiddenException("Biletul nu va apartine");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Biletul nu a fost gasit in sistemul de evenimente");
        } catch (Exception e) {
            throw new IllegalStateException("Serviciul de evenimente nu este disponibil");
        }
    }
}
