package com.example.event_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.event_service.dto.CreatePackageRequest;
import com.example.event_service.dto.PackageResponse;
import com.example.event_service.model.EventPackage;
import com.example.event_service.service.EventService;
import com.example.event_service.service.PackageService;
import com.example.event_service.service.TokenValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public PackageController(PackageService packageService,
                             TokenValidationService tokenValidationService) {
        this.packageService = packageService;
        this.tokenValidationService = tokenValidationService;
    }

    @GetMapping
    public ResponseEntity<List<EntityModel<PackageResponse>>> listPackages(
            @PathVariable Long eventId,    @RequestHeader("Authorization") String authHeader) {

        List<EntityModel<PackageResponse>> models = packageService.listPackages(eventId)
                .stream()
                .map(pkg -> toModel(eventId, pkg,authHeader))
                .toList();
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{packageId}")
    public ResponseEntity<EntityModel<PackageResponse>> getPackage(
            @PathVariable Long eventId,
            @PathVariable Long packageId,
            @RequestHeader(value = "Authorization", required = false) String authHeader ) {

        return ResponseEntity.ok(toModel(eventId, packageService.getPackage(eventId, packageId), authHeader));
    }

    @PostMapping
    public ResponseEntity<EntityModel<PackageResponse>> createPackage(
            @PathVariable Long eventId,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreatePackageRequest request) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        EventPackage pkg = packageService.createPackage(eventId, request, auth.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(eventId, pkg, authHeader));
    }

    @PutMapping("/{packageId}")
    public ResponseEntity<EntityModel<PackageResponse>> updatePackage(
            @PathVariable Long eventId,
            @PathVariable Long packageId,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreatePackageRequest request) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        EventPackage pkg = packageService.updatePackage(eventId, packageId, request, auth.getUserId());
        return ResponseEntity.ok(toModel(eventId, pkg,authHeader));
    }

    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> deletePackage(
            @PathVariable Long eventId,
            @PathVariable Long packageId,
            @RequestHeader("Authorization") String authHeader) {

        ValidateResponse auth = tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN");
        packageService.deletePackage(eventId, packageId, auth.getUserId());
        return ResponseEntity.noContent().build();
    }


    private EntityModel<PackageResponse> toModel(Long eventId, EventPackage pkg, String authHeader) {
        PackageResponse response = PackageResponse.from(pkg, packageService.getAvailableSeats(pkg.getEventPackageId(), pkg.getSeatCount()));
        response.add(Link.of("/events/" + eventId + "/packages/" + pkg.getEventPackageId()).withSelfRel());
        response.add(Link.of("/events/" + eventId).withRel("event"));
        try {
            if (tokenValidationService.requireRole(authHeader, "OWNER_EVENT", "ADMIN") != null) {
                response.add(Link.of(".../packages/" + pkg.getEventPackageId()).withRel("edit-package"));
                response.add(Link.of(".../packages/" + pkg.getEventPackageId()).withRel("delete-package"));
            }
            if (tokenValidationService.requireRole(authHeader, "CLIENT") != null) {
                response.add(Link.of(".../packages/" + pkg.getEventPackageId() + "/tickets").withRel("purchase"));
            }
        } catch (Exception e) {

        }
        return EntityModel.of(response);
    }
}
