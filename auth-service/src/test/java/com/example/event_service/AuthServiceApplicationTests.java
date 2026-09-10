package com.example.event_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: the whole application context must wire up — JPA, the Redis-backed token
 * blacklist, the gRPC server and the admin seeder. The database comes from a throwaway
 * container; jwt.secret has no default in application.properties, so a throwaway 32-byte
 * key is supplied here. Redis is not needed at startup because the connection factory
 * only opens a connection on first use.
 */
@SpringBootTest(properties =
        "jwt.secret=00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")
@Testcontainers
class AuthServiceApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void contextLoads() {
    }
}
