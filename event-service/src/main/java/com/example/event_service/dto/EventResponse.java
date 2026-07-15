package com.example.event_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter @Setter
public class EventResponse extends RepresentationModel<EventResponse> {
    @JsonProperty("eventResponseId")
    private Long eventResponseId;
    private String name;
    private String description;
    private String location;
}
