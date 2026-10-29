package com.example.event_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreatePackageRequest {

    @NotBlank
    private String name;
    private String description;
    private String location;

    @NotNull @Min(1)
    private Integer seatCount;
}
