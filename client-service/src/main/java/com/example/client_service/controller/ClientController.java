package com.example.client_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.client_service.dto.AddTicketRequest;
import com.example.client_service.dto.ClientResponse;
import com.example.client_service.dto.CreateClientRequest;
import com.example.client_service.dto.UpdateClientRequest;
import com.example.client_service.service.ClientService;
import com.example.client_service.service.ClientTicketService;
import com.example.client_service.service.TokenValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {

    private final ClientService clientService;
    private final ClientTicketService clientTicketService;
    private final TokenValidationService tokenValidationService;

    @Autowired
    public ClientController(ClientService clientService, ClientTicketService clientTicketService, TokenValidationService tokenValidationService) {
        this.clientService = clientService;
        this.clientTicketService = clientTicketService;
        this.tokenValidationService = tokenValidationService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody CreateClientRequest request) {
        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "CLIENT");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.createClient(request, auth.getUserId()));
    }

    @GetMapping("/{email}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable String email, @RequestHeader("Authorization") String authHeader) {
        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "CLIENT", "OWNER_EVENT");
        return ResponseEntity.ok(clientService.getClient(email, auth.getUserId(), auth.getRole()));
    }

    @PatchMapping("/{email}")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable String email, @RequestHeader("Authorization") String authHeader, @RequestBody UpdateClientRequest request) {
        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "CLIENT");
        return ResponseEntity.ok(clientService.updateClient(email, request, auth.getUserId()));
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteClient(@PathVariable String email, @RequestHeader("Authorization") String authHeader) {
        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "CLIENT");
        clientService.deleteClient(email, auth.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{email}/tickets")
    public ResponseEntity<List<String>> getTickets(@PathVariable String email, @RequestHeader("Authorization") String authHeader) {
        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "CLIENT");
        return ResponseEntity.ok(clientTicketService.getTickets(email, auth.getUserId()));
    }

    @PostMapping("/{email}/tickets")
    public ResponseEntity<List<String>> addTicket(@PathVariable String email, @RequestHeader("Authorization") String authHeader, @Valid @RequestBody AddTicketRequest request) {
        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "CLIENT");
        return ResponseEntity.ok(clientTicketService.addTicket(email, request.getTicketId(), auth.getUserId()));
    }
}
