package com.example.event_service.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter @Setter
public class EventResponse extends RepresentationModel<EventResponse> {
    private Long EventResponseId;
    private String name;
    private String description;
    private String location;
}
