package com.example.event_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter @Setter
public class PackageResponse extends RepresentationModel<PackageResponse> {
    @JsonProperty("packageResponseId")
    private Long packageResponseId;
    private String name;
    private String description;
    private String location;
    private Integer seatCount;
    private int availableSeats;
}
