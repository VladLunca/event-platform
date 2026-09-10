package com.example.event_service.service;

import com.example.event_service.exception.ForbiddenException;
import com.example.event_service.exception.NotFoundException;
import com.example.event_service.model.Event;
import com.example.event_service.model.EventPackage;
import com.example.event_service.model.Ticket;
import com.example.event_service.repository.EventPackageRepository;
import com.example.event_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long PACKAGE_ID = 10L;

    @Mock
    TicketRepository ticketRepository;

    @Mock
    EventPackageRepository eventPackageRepository;

    @InjectMocks
    TicketService ticketService;

    private EventPackage pkg;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        Event event = new Event();
        event.setEventId(EVENT_ID);
        event.setName("Concert");
        event.setOwnerUserId("owner-1");

        pkg = new EventPackage();
        pkg.setEventPackageId(PACKAGE_ID);
        pkg.setName("VIP");
        pkg.setSeatCount(1);
        pkg.setEvent(event);

        ticket = Ticket.create(pkg, "client-1");
    }

    private void ticketExists() {
        when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));
    }


    @Test
    void clientCanSeeOwnTicket() {
        ticketExists();

        assertThat(ticketService.getTicketDetail(ticket.getTicketId(), "client-1", "CLIENT")
                .getOwnerUserId()).isEqualTo("client-1");
    }

    @Test
    void clientCannotSeeSomeoneElsesTicket() {
        ticketExists();

        assertThatThrownBy(() ->
                ticketService.getTicketDetail(ticket.getTicketId(), "client-2", "CLIENT"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void eventOwnerCanSeeTicketOfOwnEvent() {
        ticketExists();

        assertThat(ticketService.getTicketDetail(ticket.getTicketId(), "owner-1", "OWNER_EVENT")
                .getEventName()).isEqualTo("Concert");
    }

    @Test
    void otherEventOwnerCannotSeeTicket() {
        ticketExists();

        assertThatThrownBy(() ->
                ticketService.getTicketDetail(ticket.getTicketId(), "owner-2", "OWNER_EVENT"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminCanSeeAnyTicket() {
        ticketExists();
        assertThat(ticketService.getTicketDetail(ticket.getTicketId(), "someone-else", "ADMIN"))
                .isNotNull();
    }

    @Test
    void unknownRoleIsRejected() {
        ticketExists();
        assertThatThrownBy(() ->
                ticketService.getTicketDetail(ticket.getTicketId(), "client-1", "GUEST"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void missingTicketIsNotFound() {
        when(ticketRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketDetail("nope", "client-1", "CLIENT"))
                .isInstanceOf(NotFoundException.class);
    }


    @Test
    void soldOutPackageRejectsPurchase() {
        when(eventPackageRepository.findByIdForUpdate(PACKAGE_ID)).thenReturn(Optional.of(pkg));
        when(ticketRepository.countByEventPackage_EventPackageId(PACKAGE_ID)).thenReturn(1L);

        assertThatThrownBy(() -> ticketService.purchaseTicket(EVENT_ID, PACKAGE_ID, "client-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nu mai sunt locuri");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void freeSeatIsSold() {
        when(eventPackageRepository.findByIdForUpdate(PACKAGE_ID)).thenReturn(Optional.of(pkg));
        when(ticketRepository.countByEventPackage_EventPackageId(PACKAGE_ID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket sold = ticketService.purchaseTicket(EVENT_ID, PACKAGE_ID, "client-2");

        assertThat(sold.getOwnerUserId()).isEqualTo("client-2");
        assertThat(sold.getEventPackage()).isSameAs(pkg);
    }


    @Test
    void purchaseLoadsThePackageWithALock() {
        when(eventPackageRepository.findByIdForUpdate(PACKAGE_ID)).thenReturn(Optional.of(pkg));
        when(ticketRepository.countByEventPackage_EventPackageId(PACKAGE_ID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ticketService.purchaseTicket(EVENT_ID, PACKAGE_ID, "client-2");

        verify(eventPackageRepository).findByIdForUpdate(PACKAGE_ID);
        verify(eventPackageRepository, never()).findById(any());
    }

    @Test
    void purchaseOnPackageFromAnotherEventIsNotFound() {
        when(eventPackageRepository.findByIdForUpdate(PACKAGE_ID)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> ticketService.purchaseTicket(999L, PACKAGE_ID, "client-2"))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }
}
