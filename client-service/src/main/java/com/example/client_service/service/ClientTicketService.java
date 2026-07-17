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

    public List<String> addTicket(String email, String ticketId, String userId) {
        Client client = clientRepository.findByEmail(email).orElseThrow(()->new NotFoundException("Profilul nu a fost gasit"));
        if (!client.getUserId().equals(userId)) {
            throw new ForbiddenException("Nu aveti permisiunea de a adauga bilete");
        }
        eventVerificationService.verifyTicketOwnership(ticketId, userId);
        if (!client.getTickets().contains(ticketId)) {
            client.getTickets().add(ticketId);
            clientRepository.save(client);
        }
        return client.getTickets();
    }
}
