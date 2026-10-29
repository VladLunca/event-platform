package com.example.event_service.service;

import com.example.event_service.dto.CreatePackageRequest;
import com.example.event_service.exception.ForbiddenException;
import com.example.event_service.exception.NotFoundException;
import com.example.event_service.model.Event;
import com.example.event_service.model.EventPackage;
import com.example.event_service.repository.EventPackageRepository;
import com.example.event_service.repository.EventRepository;
import com.example.event_service.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PackageService {

    private final EventPackageRepository packageRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    @Autowired
    public PackageService(EventPackageRepository packageRepository,
                          EventRepository eventRepository,
                          TicketRepository ticketRepository) {
        this.packageRepository = packageRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    public int getAvailableSeats(Long packageId, int seatCount) {
        long sold = ticketRepository.countByEventPackage_EventPackageId(packageId);
        return (int) (seatCount - sold);
    }

    public EventPackage getPackage(Long eventId, Long packageId) {
        EventPackage pkg = packageRepository.findById(packageId).orElseThrow(() -> new NotFoundException("Pachetul nu a fost gasit"));
        if (!pkg.getEvent().getEventId().equals(eventId)) {
            throw new NotFoundException("Pachetul nu apartine acestui eveniment");
        }
        return pkg;
    }

    public List<EventPackage> listPackages(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Evenimentul nu a fost gasit");
        }
        return packageRepository.findAllByEvent_EventId(eventId);
    }

    private void checkOwnership(Long eventId, String userId, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }
        if (!eventRepository.existsByEventIdAndOwnerUserId(eventId, userId)) {
            throw new ForbiddenException("Nu detineti acest eveniment");
        }
    }

    @Transactional
    public EventPackage createPackage(Long eventId, CreatePackageRequest request, String userId, String role) {
        checkOwnership(eventId, userId, role);
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Evenimentul nu a fost gasit"));
        EventPackage pkg = new EventPackage();
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setLocation(request.getLocation());
        pkg.setSeatCount(request.getSeatCount());
        pkg.setEvent(event);
        return packageRepository.save(pkg);
    }

    @Transactional
    public EventPackage updatePackage(Long eventId, Long packageId, CreatePackageRequest request, String userId, String role) {
        checkOwnership(eventId, userId, role);
        EventPackage pkg = getPackage(eventId, packageId);
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setLocation(request.getLocation());
        pkg.setSeatCount(request.getSeatCount());
        return packageRepository.save(pkg);
    }

    @Transactional
    public void deletePackage(Long eventId, Long packageId, String userId, String role) {
        checkOwnership(eventId, userId, role);
        packageRepository.delete(getPackage(eventId, packageId));
    }
}
