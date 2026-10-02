package com.example.auth_service.controller;

import com.example.auth.grpc.ValidateResponse;
import com.example.auth_service.model.Event;
import com.example.auth_service.model.EventPackage;
import com.example.auth_service.service.EventService;
import com.example.auth_service.service.PackageService;
import com.example.auth_service.service.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackageController.class)
class PackageControllerTest {

    private static final long EVENT_ID = 1L;
    private static final long PACKAGE_ID = 3L;
    private static final String OWNER_ID = "owner-1";
    private static final String OWNER_TOKEN = "Bearer owner-token";
    private static final String CLIENT_TOKEN = "Bearer client-token";

    private static final String PACKAGE = "$._embedded.packages[0]";
    private static final String LINKS = PACKAGE + "._links";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PackageService packageService;

    @MockitoBean
    TokenValidationService tokenValidationService;

    @MockitoBean
    EventService eventService;

    @BeforeEach
    void stubEventWithOnePackage() {
        Event event = new Event();
        event.setEventId(EVENT_ID);
        event.setName("Concert Iasi");
        event.setOwnerUserId(OWNER_ID);

        EventPackage pkg = new EventPackage();
        pkg.setEventPackageId(PACKAGE_ID);
        pkg.setName("VIP");
        pkg.setSeatCount(50);
        pkg.setEvent(event);

        when(eventService.getEvent(EVENT_ID)).thenReturn(event);
        when(packageService.listPackages(EVENT_ID)).thenReturn(List.of(pkg));
        when(packageService.getPackage(EVENT_ID, PACKAGE_ID)).thenReturn(pkg);
        when(packageService.getAvailableSeats(anyLong(), anyInt())).thenReturn(47);
        when(tokenValidationService.validateSilently(any())).thenReturn(Optional.empty());
        when(tokenValidationService.validateSilently(OWNER_TOKEN))
                .thenReturn(Optional.of(authenticatedAs(OWNER_ID, "OWNER_EVENT")));
        when(tokenValidationService.validateSilently(CLIENT_TOKEN))
                .thenReturn(Optional.of(authenticatedAs("client-9", "CLIENT")));
    }

    private static ValidateResponse authenticatedAs(String userId, String role) {
        return ValidateResponse.newBuilder()
                .setValid(true)
                .setUserId(userId)
                .setRole(role)
                .build();
    }

    @Test
    void packageListIsReadableWithoutAnyToken() throws Exception {
        mockMvc.perform(get("/events/{eventId}/packages", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PACKAGE + ".name").value("VIP"))
                .andExpect(jsonPath(PACKAGE + ".seatCount").value(50))
                .andExpect(jsonPath(PACKAGE + ".availableSeats").value(47));
    }

    @Test
    void singlePackageIsReadableWithoutAnyToken() throws Exception {
        mockMvc.perform(get("/events/{eventId}/packages/{packageId}", EVENT_ID, PACKAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VIP"));
    }

    @Test
    void anonymousListingCarriesNavigationLinksButNoManagementOrPurchaseLinks() throws Exception {
        mockMvc.perform(get("/events/{eventId}/packages", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LINKS + ".self.href").value("/events/1/packages/3"))
                .andExpect(jsonPath(LINKS + ".event.href").value("/events/1"))
                .andExpect(jsonPath(LINKS + "['edit-package']").doesNotExist())
                .andExpect(jsonPath(LINKS + "['delete-package']").doesNotExist())
                .andExpect(jsonPath(LINKS + ".tickets").doesNotExist())
                .andExpect(jsonPath(LINKS + ".purchase").doesNotExist());
    }

    @Test
    void ownerListingCarriesTheManagementLinks() throws Exception {
        mockMvc.perform(get("/events/{eventId}/packages", EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, OWNER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LINKS + "['edit-package']").exists())
                .andExpect(jsonPath(LINKS + "['delete-package']").exists())
                .andExpect(jsonPath(LINKS + ".tickets").exists())
                .andExpect(jsonPath(LINKS + ".purchase").doesNotExist());
    }

    @Test
    void clientListingCarriesThePurchaseLinkOnly() throws Exception {
        mockMvc.perform(get("/events/{eventId}/packages", EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, CLIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LINKS + ".purchase.href").value("/events/1/packages/3/tickets"))
                .andExpect(jsonPath(LINKS + "['edit-package']").doesNotExist())
                .andExpect(jsonPath(LINKS + ".tickets").doesNotExist());
    }

    @Test
    void anExpiredOrInvalidTokenDegradesToTheAnonymousViewInsteadOfFailing() throws Exception {
        mockMvc.perform(get("/events/{eventId}/packages", EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LINKS + ".self.href").value("/events/1/packages/3"))
                .andExpect(jsonPath(LINKS + "['edit-package']").doesNotExist());
    }
}
