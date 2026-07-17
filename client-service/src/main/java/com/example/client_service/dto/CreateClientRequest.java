package com.example.client_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClientRequest {

    @NotBlank
    @Email
    private String email;

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
