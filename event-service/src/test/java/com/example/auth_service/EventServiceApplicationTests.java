package com.example.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: the whole application context must wire up — entities, repositories,
 * controllers, the gRPC client and the HATEOAS assemblers. The database comes from a
 * throwaway container, so this does not depend on a locally running compose stack
 * (whose published port, schema name and credentials differ from the local defaults).
 */
@SpringBootTest
@Testcontainers
class EventServiceApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void contextLoads() {
    }
}
