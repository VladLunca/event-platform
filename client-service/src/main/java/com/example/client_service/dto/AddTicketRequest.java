package com.example.client_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTicketRequest {

    @NotBlank
    private String ticketId;
}
