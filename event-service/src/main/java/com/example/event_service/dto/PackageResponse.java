package com.example.event_service.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter @Setter
public class PackageResponse extends RepresentationModel<PackageResponse> {
    private Long PackageResponseId;
    private String name;
    private String description;
    private String location;
    private Integer seatCount;
    private int availableSeats;
}
