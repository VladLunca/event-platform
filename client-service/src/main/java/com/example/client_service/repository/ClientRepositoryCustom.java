package com.example.client_service.repository;

import com.example.client_service.model.Client;

import java.util.Optional;

public interface ClientRepositoryCustom {

    Optional<Client> addTicket(String email, String ticketId);
}
