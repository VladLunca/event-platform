package com.example.auth_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.auth_service.dto.TicketDetailResponse;
import com.example.auth_service.exception.ForbiddenException;
import com.example.auth_service.exception.NotFoundException;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.service.TicketService;
import com.example.auth_service.service.TokenValidationService;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketLookupController.class)
class
TicketLookupControllerTest {

    private static final String TICKET_ID = "b4f1-ticket";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TicketService ticketService;

    @MockitoBean
    TokenValidationService tokenValidationService;

    private ValidateResponse authenticatedAs(String userId, String role) {
        return ValidateResponse.newBuilder()
                .setValid(true)
                .setUserId(userId)
                .setRole(role)
                .build();
    }

    private TicketDetailResponse ticketDetail() {
        TicketDetailResponse detail = new TicketDetailResponse();
        detail.setTicketId(TICKET_ID);
        detail.setOwnerUserId("client-1");
        detail.setEventName("Concert");
        return detail;
    }

    @Test
    void requestWithoutAuthorizationHeaderNeverReachesTheService() throws Exception {
        mockMvc.perform(get("/events/tickets/{id}", TICKET_ID))
                .andExpect(status().isBadRequest());

        verify(ticketService, never()).getTicketDetail(anyString(), anyString(), anyString());
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        when(tokenValidationService.requireAuth(anyString()))
                .thenThrow(new UnauthorizedException("Token invalid sau expirat"));

        mockMvc.perform(get("/events/tickets/{id}", TICKET_ID)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).getTicketDetail(anyString(), anyString(), anyString());
    }

    @Test
    void foreignTicketIsForbidden() throws Exception {
        when(tokenValidationService.requireAuth(anyString()))
                .thenReturn(authenticatedAs("client-2", "CLIENT"));
        when(ticketService.getTicketDetail(TICKET_ID, "client-2", "CLIENT"))
                .thenThrow(new ForbiddenException("Nu aveti permisiunea de a vedea acest bilet"));

        mockMvc.perform(get("/events/tickets/{id}", TICKET_ID)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerGetsTheTicket() throws Exception {
        when(tokenValidationService.requireAuth(anyString()))
                .thenReturn(authenticatedAs("client-1", "CLIENT"));
        when(ticketService.getTicketDetail(TICKET_ID, "client-1", "CLIENT"))
                .thenReturn(ticketDetail());

        mockMvc.perform(get("/events/tickets/{id}", TICKET_ID)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUserId").value("client-1"))
                .andExpect(jsonPath("$.eventName").value("Concert"));
    }

    @Test
    void identityFromTheTokenIsPassedToTheService() throws Exception {
        when(tokenValidationService.requireAuth(anyString()))
                .thenReturn(authenticatedAs("owner-1", "OWNER_EVENT"));
        when(ticketService.getTicketDetail(TICKET_ID, "owner-1", "OWNER_EVENT"))
                .thenReturn(ticketDetail());

        mockMvc.perform(get("/events/tickets/{id}", TICKET_ID)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk());

        verify(ticketService).getTicketDetail(eq(TICKET_ID), eq("owner-1"), eq("OWNER_EVENT"));
    }

    @Test
    void authServiceFailureIsServiceUnavailableNotInternalError() throws Exception {
        when(tokenValidationService.requireAuth(anyString()))
                .thenThrow(Status.DEADLINE_EXCEEDED.asRuntimeException());

        mockMvc.perform(get("/events/tickets/{id}", TICKET_ID)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void unknownTicketIsNotFound() throws Exception {
        when(tokenValidationService.requireAuth(anyString()))
                .thenReturn(authenticatedAs("client-1", "CLIENT"));
        when(ticketService.getTicketDetail(anyString(), anyString(), anyString()))
                .thenThrow(new NotFoundException("Biletul nu a fost gasit"));

        mockMvc.perform(get("/events/tickets/{id}", TICKET_ID)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isNotFound());
    }
}
