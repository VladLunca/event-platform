package com.example.event_service.dto;

import com.example.event_service.model.EventPackage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Getter @Setter
@Relation(collectionRelation = "packages")
public class PackageResponse extends RepresentationModel<PackageResponse> {
    @JsonProperty("packageResponseId")
    private Long packageResponseId;
    private String name;
    private String description;
    private String location;
    private Integer seatCount;
    private int availableSeats;

    public static PackageResponse from(EventPackage pkg, int availableSeats) {
        PackageResponse response = new PackageResponse();
        response.setPackageResponseId(pkg.getEventPackageId());
        response.setName(pkg.getName());
        response.setDescription(pkg.getDescription());
        response.setLocation(pkg.getLocation());
        response.setSeatCount(pkg.getSeatCount());
        response.setAvailableSeats(availableSeats);
        return response;
    }
}
