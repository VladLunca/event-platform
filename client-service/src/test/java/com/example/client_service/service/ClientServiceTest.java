package com.example.client_service.service;

import com.example.client_service.dto.ClientResponse;
import com.example.client_service.dto.CreateClientRequest;
import com.example.client_service.exception.ForbiddenException;
import com.example.client_service.model.Client;
import com.example.client_service.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientServiceTest {

    private static final String MY_EMAIL = "client@platform.com";
    private static final String MY_USER_ID = "42";

    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final ClientService clientService = new ClientService(clientRepository);

    @BeforeEach
    void repositoryAcceptsAnySave() {
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static CreateClientRequest requestFor(String email) {
        CreateClientRequest request = new CreateClientRequest();
        request.setEmail(email);
        request.setFirstName("Ion");
        return request;
    }

    @Test
    void profileForTheCallersOwnEmailIsCreated() {
        ClientResponse response =
                clientService.createClient(requestFor(MY_EMAIL), MY_USER_ID, MY_EMAIL);

        assertThat(response.getEmail()).isEqualTo(MY_EMAIL);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void profileForSomeoneElsesEmailIsRefused() {
        assertThatThrownBy(() -> clientService.createClient(
                requestFor("victim@platform.com"), MY_USER_ID, MY_EMAIL))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("propriul email");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void theEmailIsComparedIgnoringCase() {
        ClientResponse response = clientService.createClient(
                requestFor("Client@Platform.COM"), MY_USER_ID, MY_EMAIL);

        assertThat(response.getEmail()).isEqualTo("Client@Platform.COM");
    }

    @Test
    void anIdentityWithoutAnEmailIsRefusedRatherThanTrustingTheBody() {
        assertThatThrownBy(() -> clientService.createClient(requestFor(MY_EMAIL), MY_USER_ID, null))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> clientService.createClient(requestFor(MY_EMAIL), MY_USER_ID, ""))
                .isInstanceOf(ForbiddenException.class);

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void theStoredUserIdComesFromTheTokenNotFromTheBody() {
        clientService.createClient(requestFor(MY_EMAIL), MY_USER_ID, MY_EMAIL);

        verify(clientRepository).save(org.mockito.ArgumentMatchers.argThat(
                saved -> MY_USER_ID.equals(saved.getUserId())));
    }

    @Test
    void aSecondProfileForTheSameEmailIsStillAConflict() {
        when(clientRepository.existsByEmail(MY_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(requestFor(MY_EMAIL), MY_USER_ID, MY_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exista deja un profil");
    }
}
