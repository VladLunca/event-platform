package com.example.event_service.dto;

import com.example.event_service.model.Event;
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

    public static EventResponse from(Event event) {
        EventResponse response = new EventResponse();
        response.setEventResponseId(event.getEventId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setLocation(event.getLocation());
        return response;
    }
}
