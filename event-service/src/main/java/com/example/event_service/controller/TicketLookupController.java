package com.example.event_service.controller;

import com.example.event_service.dto.TicketResponse;
import com.example.event_service.exception.NotFoundException;
import com.example.event_service.model.Ticket;
import com.example.event_service.repository.TicketRepository;
import com.example.event_service.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/tickets")
public class TicketLookupController {

    private final TicketService ticketService;

    @Autowired
    public TicketLookupController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable String ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        TicketResponse response = new TicketResponse();
        response.setTicketResponseId(ticket.getTicketId());
        response.setOwnerUserId(ticket.getOwnerUserId());
        return ResponseEntity.ok(response);
    }
}
