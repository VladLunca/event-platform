package com.example.client_service.service;

import com.example.client_service.dto.ClientResponse;
import com.example.client_service.dto.CreateClientRequest;
import com.example.client_service.dto.UpdateClientRequest;
import com.example.client_service.exception.ForbiddenException;
import com.example.client_service.exception.NotFoundException;
import com.example.client_service.model.Client;
import com.example.client_service.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    @Autowired
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client findByEmail(String email) {
        return clientRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("Profilul nu a fost gasit"));
    }

    private void checkOwnership(Client client, String userId) {
        if (!client.getUserId().equals(userId)) {
            throw new ForbiddenException("Nu aveti permisiunea de a modifica acest profil");
        }
    }

    public ClientResponse createClient(CreateClientRequest request, String userId) {
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Exista deja un profil pentru acest email");
        }
        Client client = new Client();
        client.setEmail(request.getEmail());
        client.setUserId(userId);
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setPublicInfo(request.getPublicInfo() != null && request.getPublicInfo());

        if (request.getSocialMedia() != null) {
            Client.SocialMedia sm = new Client.SocialMedia();
            sm.setLinkedin(request.getSocialMedia().getLinkedin());
            sm.setPublicSocialMedia(request.getSocialMedia().getPublicSocialMedia() != null
                    && request.getSocialMedia().getPublicSocialMedia());
            client.setSocialMedia(sm);
        }

        return ClientResponse.from(clientRepository.save(client));
    }

    public ClientResponse getClient(String email, String userId, String role) {
        Client client = findByEmail(email);
        if ("CLIENT".equals(role) && client.getUserId().equals(userId)) {
            return ClientResponse.from(client);
        }
        if ("OWNER_EVENT".equals(role)) {
            return ClientResponse.fromPublic(client);
        }
        throw new ForbiddenException("Nu aveti permisiunea de a vedea acest profil");
    }

    public ClientResponse updateClient(String email, UpdateClientRequest request, String userId) {
        Client client = findByEmail(email);
        checkOwnership(client, userId);

        if (request.getFirstName() != null) {
            client.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null){
            client.setLastName(request.getLastName());
        }
        if (request.getPublicInfo() != null) {
            client.setPublicInfo(request.getPublicInfo());
        }

        if (request.getSocialMedia() != null) {
            Client.SocialMedia sm = (client.getSocialMedia() != null) ? client.getSocialMedia() : new Client.SocialMedia();
            if (request.getSocialMedia().getLinkedin() != null)
                sm.setLinkedin(request.getSocialMedia().getLinkedin());
            if (request.getSocialMedia().getPublicSocialMedia() != null)
                sm.setPublicSocialMedia(request.getSocialMedia().getPublicSocialMedia());
            client.setSocialMedia(sm);
        }

        return ClientResponse.from(clientRepository.save(client));
    }

    public void deleteClient(String email, String userId) {
        Client client = findByEmail(email);
        checkOwnership(client, userId);
        clientRepository.delete(client);
    }
}
