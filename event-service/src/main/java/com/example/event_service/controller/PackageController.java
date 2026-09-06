package com.example.event_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.event_service.dto.CreatePackageRequest;
import com.example.event_service.dto.PackageResponse;
import com.example.event_service.model.Event;
import com.example.event_service.model.EventPackage;
import com.example.event_service.service.EventService;
import com.example.event_service.service.PackageService;
import com.example.event_service.service.TokenValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events/{eventId}/packages")
public class PackageController {

    private final PackageService packageService;
    private final TokenValidationService tokenValidationService;
    private final EventService eventService;

    @Autowired
    public PackageController(PackageService packageService,
                             TokenValidationService tokenValidationService, EventService eventService) {
        this.packageService = packageService;
        this.tokenValidationService = tokenValidationService;
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<PackageResponse>>> listPackages(
            @PathVariable Long eventId,    @RequestHeader("Authorization") String authHeader) {

        List<EntityModel<PackageResponse>> models = packageService.listPackages(eventId)
                .stream()
                .map(pkg -> toModel(eventId, pkg,eventService.getEvent(eventId),tokenValidationService.validateSilently(authHeader).orElse(null)))
                .toList();
        return ResponseEntity.ok(CollectionModel.of(models));
    }

    @GetMapping("/{packageId}")
    public ResponseEntity<EntityModel<PackageResponse>> getPackage(
            @PathVariable Long eventId,
            @PathVariable Long packageId,
            @RequestHeader(value = "Authorization", required = false) String authHeader ) {

        return ResponseEntity.ok(toModel(eventId, packageService.getPackage(eventId, packageId),eventService.getEvent(eventId), tokenValidationService.validateSilently(authHeader).orElse(null)));
    }

    @PostMapping
    public ResponseEntity<EntityModel<PackageResponse>> createPackage(
            @PathVariable Long eventId,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreatePackageRequest request) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        EventPackage pkg = packageService.createPackage(eventId, request, auth.getUserId(), auth.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(eventId, pkg, eventService.getEvent(eventId),tokenValidationService.validateSilently(authHeader).orElse(null)));
    }

    @PutMapping("/{packageId}")
    public ResponseEntity<EntityModel<PackageResponse>> updatePackage(
            @PathVariable Long eventId,
            @PathVariable Long packageId,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreatePackageRequest request) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        EventPackage pkg = packageService.updatePackage(eventId, packageId, request, auth.getUserId(), auth.getRole());
        return ResponseEntity.ok(toModel(eventId, pkg,eventService.getEvent(eventId),tokenValidationService.validateSilently(authHeader).orElse(null)));
    }

    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> deletePackage(
            @PathVariable Long eventId,
            @PathVariable Long packageId,
            @RequestHeader("Authorization") String authHeader) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        packageService.deletePackage(eventId, packageId, auth.getUserId(), auth.getRole());
        return ResponseEntity.noContent().build();
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
    private boolean isClient(ValidateResponse auth) {
        return auth != null && "CLIENT".equals(auth.getRole());
    }
    private EntityModel<PackageResponse> toModel(Long eventId, EventPackage pkg, Event event, ValidateResponse auth) {
        int availableSeats = packageService.getAvailableSeats(pkg.getEventPackageId(), pkg.getSeatCount());
        PackageResponse response = PackageResponse.from(pkg, availableSeats);

        String packagePath = "/events/" + eventId + "/packages/" + pkg.getEventPackageId();

        response.add(Link.of(packagePath).withSelfRel());
        response.add(Link.of("/events/" + eventId).withRel("event"));

        if (canManage(event, auth)) {
            response.add(Link.of(packagePath).withRel("edit-package"));
            response.add(Link.of(packagePath).withRel("delete-package"));
            response.add(Link.of(packagePath + "/tickets").withRel("tickets"));
        }

        if (isClient(auth)) {
            response.add(Link.of(packagePath + "/tickets").withRel("purchase"));
        }

        return EntityModel.of(response);
    }
}
