package com.example.client_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "clients")
public class Client {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String userId;

    private String firstName;
    private String lastName;
    private boolean publicInfo = false;
    private SocialMedia socialMedia;
    private List<String> tickets = new ArrayList<>();

    @Getter
    @Setter
    public static class SocialMedia {
        private String linkedin;
        private boolean publicSocialMedia = false;
    }
}
