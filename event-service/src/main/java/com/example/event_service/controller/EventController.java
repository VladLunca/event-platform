package com.example.event_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.event_service.dto.CreateEventRequest;
import com.example.event_service.dto.EventResponse;
import com.example.event_service.model.Event;
import com.example.event_service.service.EventService;
import com.example.event_service.service.TokenValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final TokenValidationService tokenValidationService;

    @Autowired
    public EventController(EventService eventService, TokenValidationService tokenValidationService) {
        this.eventService = eventService;
        this.tokenValidationService = tokenValidationService;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<EventResponse>>> listEvents(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Validăm token-ul o SINGURĂ dată per cerere
        ValidateResponse auth = validateTokenSilently(authHeader);
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Event> eventPage = eventService.listEvents(name, pageable);

        List<EntityModel<EventResponse>> models = eventPage.getContent().stream()
                .map(event -> toModel(event, auth))
                .toList();

        return ResponseEntity.ok(CollectionModel.of(models));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<EventResponse>> getEvent(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        ValidateResponse auth = validateTokenSilently(authHeader);
        return ResponseEntity.ok(toModel(eventService.getEvent(id), auth));
    }

    @PostMapping
    public ResponseEntity<EntityModel<EventResponse>> createEvent(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateEventRequest request) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        Event event = eventService.createEvent(request, auth.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(event, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<EventResponse>> updateEvent(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateEventRequest request) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        Event updated = eventService.updateEvent(id, request, auth.getUserId(), auth.getRole());
        return ResponseEntity.ok(toModel(updated, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        eventService.deleteEvent(id, auth.getUserId(), auth.getRole());
        return ResponseEntity.noContent().build();
    }

    private ValidateResponse validateTokenSilently(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        try {
            return tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        } catch (Exception e) {
            // Token invalid sau expirat -> utilizatorul este tratat ca neautentificat
            return null;
        }
    }

    private boolean canManage(Event event, ValidateResponse auth) {
        if (auth == null || auth.getRole() == null) {
            return false;
        }
        boolean isAdmin = "ADMIN".equals(auth.getRole());
        boolean isOwner = "OWNER_EVENT".equals(auth.getRole())
                && event.getOwnerUserId() != null
                && event.getOwnerUserId().equals(auth.getUserId());
        return isAdmin || isOwner;
    }

    private EntityModel<EventResponse> toModel(Event event, ValidateResponse auth) {
        EventResponse response = EventResponse.from(event);
        response.add(Link.of("/events/" + event.getEventId()).withSelfRel());
        response.add(Link.of("/events/" + event.getEventId() + "/packages").withRel("packages"));

        if (canManage(event, auth)) {
            response.add(Link.of("/events/" + event.getEventId()).withRel("edit"));
            response.add(Link.of("/events/" + event.getEventId()).withRel("delete"));
            response.add(Link.of("/events/" + event.getEventId() + "/packages").withRel("create-package"));
        }

        return EntityModel.of(response);
    }
}