package com.example.client_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateClientRequest {

    private String firstName;
    private String lastName;
    private Boolean publicInfo;
    private SocialMediaDto socialMedia;

    @Getter
    @Setter
    public static class SocialMediaDto {
        private String linkedin;
        private Boolean publicSocialMedia;
    }
}
