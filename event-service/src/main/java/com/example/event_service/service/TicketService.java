package com.example.event_service.service;

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
        EventPackage pkg = resolvePackage(eventId, packageId);
        long sold = ticketRepository.countByEventPackage_EventPackageId(packageId);
        if (sold >= pkg.getSeatCount()) {
            throw new IllegalStateException("Nu mai sunt locuri disponibile");
        }
        return ticketRepository.save(Ticket.create(pkg, buyerUserId));
    }

    public Ticket getTicket(Long eventId, Long packageId, String code, String userId, String role) {
        EventPackage pkg = resolvePackage(eventId, packageId);
        Ticket ticket = ticketRepository.findById(code).orElseThrow(() -> new NotFoundException("Biletul nu a fost gasit"));
        if (!ticket.getEventPackage().getEventPackageId().equals(packageId)) {
            throw new NotFoundException("Biletul nu apartine acestui pachet");
        }
        if ("ADMIN".equals(role)) return ticket;
        if ("CLIENT".equals(role) && ticket.getOwnerUserId().equals(userId)) return ticket;
        if ("OWNER_EVENT".equals(role) && pkg.getEvent().getOwnerUserId().equals(userId)) return ticket;
        throw new ForbiddenException("Nu aveti permisiunea de a vedea acest bilet");
    }
    public Ticket getTicketById(String ticketId) {
        return ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Biletul nu a fost gasit"));
    }
}
