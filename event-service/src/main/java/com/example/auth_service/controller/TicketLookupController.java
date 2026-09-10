package com.example.auth_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.auth_service.dto.TicketDetailResponse;
import com.example.auth_service.service.TicketService;
import com.example.auth_service.service.TokenValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/tickets")
public class TicketLookupController {

    private final TicketService ticketService;
    private final TokenValidationService tokenValidationService;

    @Autowired
    public TicketLookupController(TicketService ticketService, TokenValidationService tokenValidationService) {
        this.ticketService = ticketService;
        this.tokenValidationService = tokenValidationService;
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDetailResponse> getTicketById(
            @PathVariable String ticketId,
            @RequestHeader("Authorization") String authHeader) {

        ValidateResponse auth = tokenValidationService.requireAuth(authHeader);
        return ResponseEntity.ok(
                ticketService.getTicketDetail(ticketId, auth.getUserId(), auth.getRole()));
    }
}
