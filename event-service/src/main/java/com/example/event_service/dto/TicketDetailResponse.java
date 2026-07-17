package com.example.event_service.dto;

import com.example.event_service.model.Ticket;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketDetailResponse {

    private String ticketId;
    private String ownerUserId;
    private Long eventId;
    private String eventName;
    private Long packageId;
    private String packageName;
    private int seatCount;

    public static TicketDetailResponse from(Ticket ticket) {
        TicketDetailResponse dto = new TicketDetailResponse();
        dto.setTicketId(ticket.getTicketId());
        dto.setOwnerUserId(ticket.getOwnerUserId());
        dto.setPackageId(ticket.getEventPackage().getEventPackageId());
        dto.setPackageName(ticket.getEventPackage().getName());
        dto.setSeatCount(ticket.getEventPackage().getSeatCount());
        dto.setEventId(ticket.getEventPackage().getEvent().getEventId());
        dto.setEventName(ticket.getEventPackage().getEvent().getName());
        return dto;
    }
}
