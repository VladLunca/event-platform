package com.example.event_service.service;

import com.example.event_service.dto.TicketDetailResponse;
import com.example.event_service.exception.ForbiddenException;
import com.example.event_service.exception.NotFoundException;
import com.example.event_service.model.EventPackage;
import com.example.event_service.model.Ticket;
import com.example.event_service.repository.EventPackageRepository;
import com.example.event_service.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final EventPackageRepository eventPackageRepository;

    @Autowired
    public TicketService(TicketRepository ticketRepository, EventPackageRepository eventPackageRepository) {
        this.ticketRepository = ticketRepository;
        this.eventPackageRepository = eventPackageRepository;
    }

    private EventPackage resolvePackage(Long eventId, Long packageId) {
        EventPackage pkg = eventPackageRepository.findById(packageId).orElseThrow(() -> new NotFoundException("Pachetul nu a fost gasit"));
        return checkBelongsToEvent(pkg, eventId);
    }

    private EventPackage checkBelongsToEvent(EventPackage pkg, Long eventId) {
        if (!pkg.getEvent().getEventId().equals(eventId)) {
            throw new NotFoundException("Pachetul nu apartine acestui eveniment");
        }
        return pkg;
    }

    public List<Ticket> listTickets(Long eventId, Long packageId, String userId, String role) {
        EventPackage pkg = resolvePackage(eventId, packageId);
        if ("ADMIN".equals(role) || ("OWNER_EVENT".equals(role) && pkg.getEvent().getOwnerUserId().equals(userId))) {
            return ticketRepository.findByEventPackage_EventPackageId(packageId);
        }
        throw new ForbiddenException("Nu aveti permisiunea de a vedea biletele");
    }

    @Transactional
    public Ticket purchaseTicket(Long eventId, Long packageId, String buyerUserId) {
        // The lock must be taken before counting, otherwise two concurrent buyers
        // read the same count, both see a free seat and both insert a ticket.
        EventPackage pkg = eventPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new NotFoundException("Pachetul nu a fost gasit"));
        checkBelongsToEvent(pkg, eventId);

        long sold = ticketRepository.countByEventPackage_EventPackageId(packageId);
        if (sold >= pkg.getSeatCount()) {
            throw new IllegalStateException("Nu mai sunt locuri disponibile");
        }
        return ticketRepository.save(Ticket.create(pkg, buyerUserId));
    }

    public Ticket getTicket(Long eventId, Long packageId, String code, String userId, String role) {
        resolvePackage(eventId, packageId);
        Ticket ticket = ticketRepository.findById(code).orElseThrow(() -> new NotFoundException("Biletul nu a fost gasit"));
        if (!ticket.getEventPackage().getEventPackageId().equals(packageId)) {
            throw new NotFoundException("Biletul nu apartine acestui pachet");
        }
        if (!canViewTicket(ticket, userId, role)) {
            throw new ForbiddenException("Nu aveti permisiunea de a vedea acest bilet");
        }
        return ticket;
    }
    public Ticket getTicketById(String ticketId) {
        return ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Biletul nu a fost gasit"));
    }

    /**
     * A ticket may be viewed by an admin, by the client who bought it,
     * or by the owner of the event the package belongs to.
     */
    private boolean canViewTicket(Ticket ticket, String userId, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }
        if ("CLIENT".equals(role)) {
            return ticket.getOwnerUserId().equals(userId);
        }
        if ("OWNER_EVENT".equals(role)) {
            return ticket.getEventPackage().getEvent().getOwnerUserId().equals(userId);
        }
        return false;
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketDetail(String ticketId, String userId, String role) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Biletul nu a fost gasit"));
        if (!canViewTicket(ticket, userId, role)) {
            throw new ForbiddenException("Nu aveti permisiunea de a vedea acest bilet");
        }
        return TicketDetailResponse.from(ticket);
    }
}
