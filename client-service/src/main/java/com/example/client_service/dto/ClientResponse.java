package com.example.client_service.dto;

import com.example.client_service.model.Client;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClientResponse {

    private String email;
    private String firstName;
    private String lastName;
    private Boolean publicInfo;
    private SocialMediaDto socialMedia;
    private List<String> tickets;

    @Getter
    @Setter
    public static class SocialMediaDto {
        private String linkedin;
        private Boolean publicSocialMedia;
    }

    public static ClientResponse from(Client client) {
        ClientResponse response = new ClientResponse();
        response.setEmail(client.getEmail());
        response.setFirstName(client.getFirstName());
        response.setLastName(client.getLastName());
        response.setPublicInfo(client.isPublicInfo());
        response.setTickets(client.getTickets());

        if (client.getSocialMedia() != null) {
            SocialMediaDto sm = new SocialMediaDto();
            sm.setLinkedin(client.getSocialMedia().getLinkedin());
            sm.setPublicSocialMedia(client.getSocialMedia().isPublicSocialMedia());
            response.setSocialMedia(sm);
        }

        return response;
    }

    public static ClientResponse fromPublic(Client client) {
        ClientResponse response = new ClientResponse();
        response.setEmail(client.getEmail());
        response.setPublicInfo(client.isPublicInfo());

        if (client.isPublicInfo()) {
            response.setFirstName(client.getFirstName());
            response.setLastName(client.getLastName());

            if (client.getSocialMedia() != null && client.getSocialMedia().isPublicSocialMedia()) {
                SocialMediaDto sm = new SocialMediaDto();
                sm.setLinkedin(client.getSocialMedia().getLinkedin());
                sm.setPublicSocialMedia(true);
                response.setSocialMedia(sm);
            }
        }

        return response;
    }
}
