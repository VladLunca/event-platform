package com.example.event_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name="tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {
    @Id
    private String ticketId;

    @Column(nullable = false)
    private String ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private EventPackage eventPackage;

    public static Ticket create(EventPackage pkg, String ownerUserId) {
        Ticket ticket = new Ticket();
        ticket.setTicketId(UUID.randomUUID().toString());
        ticket.setOwnerUserId(ownerUserId);
        ticket.setEventPackage(pkg);
        return ticket;
    }
}
