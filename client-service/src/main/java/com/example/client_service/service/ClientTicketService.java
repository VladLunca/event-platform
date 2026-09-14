package com.example.client_service.service;

import com.example.client_service.exception.ForbiddenException;
import com.example.client_service.exception.NotFoundException;
import com.example.client_service.model.Client;
import com.example.client_service.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientTicketService {

    private final ClientRepository clientRepository;
    private final EventVerificationService eventVerificationService;

    @Autowired
    public ClientTicketService(ClientRepository clientRepository,
                               EventVerificationService eventVerificationService) {
        this.clientRepository = clientRepository;
        this.eventVerificationService = eventVerificationService;
    }

    public List<String> getTickets(String email, String userId) {
        Client client = clientRepository.findByEmail(email).orElseThrow(()->new NotFoundException("Profilul nu a fost gasit"));
        if (!client.getUserId().equals(userId)) {
            throw new ForbiddenException("Nu aveti permisiunea de a vedea aceste bilete");
        }
        return client.getTickets();
    }

    public List<String> addTicket(String email, String ticketId, String userId, String authHeader) {
        Client client = clientRepository.findByEmail(email).orElseThrow(()->new NotFoundException("Profilul nu a fost gasit"));
        if (!client.getUserId().equals(userId)) {
            throw new ForbiddenException("Nu aveti permisiunea de a adauga bilete");
        }
        eventVerificationService.verifyTicketOwnership(ticketId, userId, authHeader);
        // Loading the profile, appending in memory and saving it back would drop a ticket
        // added concurrently: each save writes the whole document as the caller read it.
        return clientRepository.addTicket(email, ticketId)
                .orElseThrow(() -> new NotFoundException("Profilul nu a fost gasit"))
                .getTickets();
    }
}
