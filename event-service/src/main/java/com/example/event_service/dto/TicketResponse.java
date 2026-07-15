package com.example.event_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter @Setter
public class TicketResponse extends RepresentationModel<TicketResponse> {
    @JsonProperty("ticketResponseId")
    private String ticketResponseId;
    private String ownerUserId;
}
