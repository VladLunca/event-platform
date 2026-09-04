package com.example.event_service.service;

import com.example.event_service.dto.CreateEventRequest;
import com.example.event_service.exception.ForbiddenException;
import com.example.event_service.exception.NotFoundException;
import com.example.event_service.model.Event;
import com.example.event_service.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    public Page<Event> listEvents(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return eventRepository.findByNameContainingIgnoreCase(name, pageable);
        }
        return eventRepository.findAll(pageable);
    }

    public Event getEvent(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Evenimentul nu a fost gasit"));
    }

    public void checkOwnership(Long eventId, String userId) {
        if (!eventRepository.existsByEventIdAndOwnerUserId(eventId, userId)) {
            throw new ForbiddenException("Nu detineti acest eveniment");
        }
    }

    @Transactional
    public Event createEvent(CreateEventRequest request, String ownerUserId) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setOwnerUserId(ownerUserId);
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Long id, CreateEventRequest request, String userId) {
        checkOwnership(id, userId);
        Event event = getEvent(id);
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long id, String userId) {
        checkOwnership(id, userId);
        eventRepository.delete(getEvent(id));
    }

    public boolean canManageEvent(Long eventId, String authHeader) {

    }
}
