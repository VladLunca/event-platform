package com.example.event_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateEventRequest {

    @NotBlank
    private String name;
    private String description;
    private String location;
}
