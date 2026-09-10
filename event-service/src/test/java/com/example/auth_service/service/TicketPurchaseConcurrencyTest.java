package com.example.auth_service.service;

import com.example.auth_service.model.Event;
import com.example.auth_service.model.EventPackage;
import com.example.auth_service.model.Ticket;
import com.example.auth_service.repository.EventPackageRepository;
import com.example.auth_service.repository.EventRepository;
import com.example.auth_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class TicketPurchaseConcurrencyTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    TicketService ticketService;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventPackageRepository eventPackageRepository;

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    private Long eventId;
    private Long packageId;

    @BeforeEach
    void seedSingleSeatPackage() {
        ticketRepository.deleteAll();
        eventPackageRepository.deleteAll();
        eventRepository.deleteAll();

        Event event = new Event();
        event.setName("Concert");
        event.setOwnerUserId("owner-1");
        eventId = eventRepository.save(event).getEventId();

        EventPackage pkg = new EventPackage();
        pkg.setName("VIP");
        pkg.setSeatCount(1);
        pkg.setEvent(event);
        packageId = eventPackageRepository.save(pkg).getEventPackageId();
    }

    @Test
    void onlyOneOfTwoConcurrentBuyersGetsTheLastSeat() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Boolean> buy = () -> {
            start.await();
            try {
                ticketService.purchaseTicket(eventId, packageId, UUID.randomUUID().toString());
                return true;
            } catch (IllegalStateException soldOut) {
                return false;
            }
        };

        try {
            Future<Boolean> first = pool.submit(buy);
            Future<Boolean> second = pool.submit(buy);
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }

        assertThat(ticketRepository.countByEventPackage_EventPackageId(packageId)).isEqualTo(1L);
    }

    @Test
    void tenConcurrentBuyersNeverExceedTheSeatCount() throws Exception {
        int seats = 3;
        EventPackage pkg = eventPackageRepository.findById(packageId).orElseThrow();
        pkg.setSeatCount(seats);
        eventPackageRepository.save(pkg);

        int buyers = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(buyers);

        Callable<Boolean> buy = () -> {
            start.await();
            try {
                ticketService.purchaseTicket(eventId, packageId, UUID.randomUUID().toString());
                return true;
            } catch (IllegalStateException soldOut) {
                return false;
            }
        };

        try {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < buyers; i++) {
                results.add(pool.submit(buy));
            }
            start.countDown();

            long sold = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    sold++;
                }
            }
            assertThat(sold).isEqualTo(seats);
        } finally {
            pool.shutdownNow();
        }

        assertThat(ticketRepository.countByEventPackage_EventPackageId(packageId))
                .isEqualTo((long) seats);
    }

    /**
     * Control test. It reproduces the unsafe count-then-insert that purchaseTicket used
     * before the lock, running it against the same database and thread schedule as the
     * tests above. If this one does NOT oversell, the harness cannot expose the race and
     * the two tests above prove nothing, so a green run here is what gives them meaning.
     */
    @Test
    void withoutTheLockTheSameScenarioOversells() throws Exception {
        int buyers = 12;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(buyers);

        Callable<Boolean> buyWithoutLock = () -> {
            start.await();
            return unsafePurchase();
        };

        try {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < buyers; i++) {
                results.add(pool.submit(buyWithoutLock));
            }
            start.countDown();
            for (Future<Boolean> result : results) {
                result.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(ticketRepository.countByEventPackage_EventPackageId(packageId))
                .as("unsynchronized count-then-insert must exceed the single available seat")
                .isGreaterThan(1L);
    }

    private boolean unsafePurchase() {
        return transactionTemplate.execute(status -> {
            EventPackage pkg = eventPackageRepository.findById(packageId).orElseThrow();
            long sold = ticketRepository.countByEventPackage_EventPackageId(packageId);
            if (sold >= pkg.getSeatCount()) {
                return false;
            }
            sleepBriefly();
            ticketRepository.save(Ticket.create(pkg, UUID.randomUUID().toString()));
            return true;
        });
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
