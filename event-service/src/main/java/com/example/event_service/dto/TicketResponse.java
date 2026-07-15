package com.example.event_service.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter @Setter
public class TicketResponse extends RepresentationModel<TicketResponse> {
    private String TicketResponseId;
    private String ownerUserId;
}
