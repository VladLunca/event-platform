package com.example.event_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.event_service.dto.TicketResponse;
import com.example.event_service.model.Ticket;
import com.example.event_service.service.TicketService;
import com.example.event_service.service.TokenValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events/{eventId}/packages/{packageId}/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TokenValidationService tokenValidationService;

    @Autowired
    public TicketController(TicketService ticketService, TokenValidationService tokenValidationService) {
        this.ticketService = ticketService;
        this.tokenValidationService = tokenValidationService;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<TicketResponse>>> listTickets(@PathVariable Long eventId, @PathVariable Long packageId, @RequestHeader("Authorization") String authHeader) {

        ValidateResponse auth = tokenValidationService.requireAuth(authHeader);
        List<EntityModel<TicketResponse>> models = ticketService
                .listTickets(eventId, packageId, auth.getUserId(), auth.getRole())
                .stream()
                .map(t -> toModel(eventId, packageId, t))
                .toList();
        return ResponseEntity.ok(CollectionModel.of(models));
    }

    @PostMapping
    public ResponseEntity<EntityModel<TicketResponse>> purchaseTicket(@PathVariable Long eventId, @PathVariable Long packageId, @RequestHeader("Authorization") String authHeader) {
        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "CLIENT");
        Ticket ticket = ticketService.purchaseTicket(eventId, packageId, auth.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(eventId, packageId, ticket));
    }

    @GetMapping("/{code}")
    public ResponseEntity<EntityModel<TicketResponse>> getTicket(@PathVariable Long eventId, @PathVariable Long packageId, @PathVariable String code, @RequestHeader("Authorization") String authHeader) {
        ValidateResponse auth = tokenValidationService.requireAuth(authHeader);
        Ticket ticket = ticketService.getTicket(eventId, packageId, code, auth.getUserId(), auth.getRole());
        return ResponseEntity.ok(toModel(eventId, packageId, ticket));
    }

    private EntityModel<TicketResponse> toModel(Long eventId, Long packageId, Ticket ticket) {
        TicketResponse response = TicketResponse.from(ticket);
        response.add(Link.of("/events/" + eventId + "/packages/" + packageId + "/tickets/" + ticket.getTicketId()).withSelfRel());
        response.add(Link.of("/events/" + eventId + "/packages/" + packageId).withRel("package"));
        return EntityModel.of(response);
    }
}
