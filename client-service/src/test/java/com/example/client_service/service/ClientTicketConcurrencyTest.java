package com.example.client_service.service;

import com.example.client_service.model.Client;
import com.example.client_service.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ClientTicketConcurrencyTest {

    private static final String EMAIL = "client@platform.com";
    private static final String USER_ID = "user-1";
    private static final String AUTH_HEADER = "Bearer token";

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    ClientTicketService clientTicketService;

    @Autowired
    ClientRepository clientRepository;

    @MockitoBean
    EventVerificationService eventVerificationService;

    @BeforeEach
    void seedEmptyProfile() {
        clientRepository.deleteAll();

        Client client = new Client();
        client.setEmail(EMAIL);
        client.setUserId(USER_ID);
        clientRepository.save(client);
    }

    @Test
    void concurrentAddsOfDifferentTicketsAllSurvive() throws Exception {
        List<String> ticketIds = ticketIds(10);

        runConcurrently(ticketIds, ticketId ->
                clientTicketService.addTicket(EMAIL, ticketId, USER_ID, AUTH_HEADER));

        assertThat(storedTickets()).containsExactlyInAnyOrderElementsOf(ticketIds);
    }

    @Test
    void addingTheSameTicketConcurrentlyStoresASingleCopy() throws Exception {
        List<String> sameTicket = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sameTicket.add("ticket-1");
        }

        runConcurrently(sameTicket, ticketId ->
                clientTicketService.addTicket(EMAIL, ticketId, USER_ID, AUTH_HEADER));

        assertThat(storedTickets()).containsExactly("ticket-1");
    }

    @Test
    void addingTheSameTicketTwiceIsIdempotent() {
        clientTicketService.addTicket(EMAIL, "ticket-1", USER_ID, AUTH_HEADER);
        List<String> tickets = clientTicketService.addTicket(EMAIL, "ticket-1", USER_ID, AUTH_HEADER);

        assertThat(tickets).containsExactly("ticket-1");
        assertThat(storedTickets()).containsExactly("ticket-1");
    }

    @Test
    void addTicketReturnsTheListAsItWasStored() {
        clientTicketService.addTicket(EMAIL, "ticket-1", USER_ID, AUTH_HEADER);
        List<String> tickets = clientTicketService.addTicket(EMAIL, "ticket-2", USER_ID, AUTH_HEADER);

        assertThat(tickets).containsExactly("ticket-1", "ticket-2");
        assertThat(storedTickets()).containsExactly("ticket-1", "ticket-2");
    }

    @Test
    void readModifyWriteLosesTicketsUnderTheSameLoad() throws Exception {
        List<String> ticketIds = ticketIds(10);

        runConcurrently(ticketIds, this::unsafeAddTicket);

        assertThat(storedTickets())
                .as("saving the whole profile back overwrites tickets added meanwhile")
                .hasSizeLessThan(ticketIds.size());
    }

    private Object unsafeAddTicket(String ticketId) {
        Client client = clientRepository.findByEmail(EMAIL).orElseThrow();
        if (!client.getTickets().contains(ticketId)) {
            sleepBriefly();
            client.getTickets().add(ticketId);
            clientRepository.save(client);
        }
        return client.getTickets();
    }

    private void runConcurrently(List<String> ticketIds, TicketAction action) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(ticketIds.size());
        try {
            List<Future<?>> results = new ArrayList<>();
            for (String ticketId : ticketIds) {
                Callable<Object> task = () -> {
                    start.await();
                    return action.apply(ticketId);
                };
                results.add(pool.submit(task));
            }
            start.countDown();
            for (Future<?> result : results) {
                result.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private List<String> storedTickets() {
        return clientRepository.findByEmail(EMAIL).orElseThrow().getTickets();
    }

    private static List<String> ticketIds(int count) {
        List<String> ticketIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ticketIds.add("ticket-" + i);
        }
        return ticketIds;
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface TicketAction {
        Object apply(String ticketId);
    }
}
