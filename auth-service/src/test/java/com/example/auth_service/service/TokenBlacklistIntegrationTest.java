package com.example.auth_service.service;

import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.LoginRequest;
import com.example.auth.grpc.LogoutRequest;
import com.example.auth.grpc.ValidateRequest;
import com.example.auth.grpc.ValidateResponse;
import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff",
        "jwt.expiration-ms=60000"
})
@AutoConfigureTestGrpcTransport
@Testcontainers
class TokenBlacklistIntegrationTest {

    private static final String SECRET =
            "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private static final long EXPIRATION_MS = 60_000L;
    private static final String EMAIL = "revocation@platform.com";
    private static final String PASSWORD = "secret123";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @Autowired
    GrpcChannelFactory channels;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    private AuthServiceGrpc.AuthServiceBlockingStub stub;
    private Long userId;

    @BeforeEach
    void seedUserAndClearBlacklist() {
        stub = AuthServiceGrpc.newBlockingStub(channels.createChannel("auth-service"));

        userId = userRepository.findByEmail(EMAIL).orElseGet(() -> {
            User user = new User();
            user.setEmail(EMAIL);
            user.setPassword(passwordEncoder.encode(PASSWORD));
            user.setRole(Role.CLIENT);
            return userRepository.save(user);
        }).getId();

        Set<String> blacklisted = redisTemplate.keys("bl:*");
        if (blacklisted != null && !blacklisted.isEmpty()) {
            redisTemplate.delete(blacklisted);
        }
    }

    @Test
    void loginIssuesATokenThatValidatesWithTheStoredIdentity() {
        String token = login();

        ValidateResponse response = validate(token);

        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(String.valueOf(userId));
        assertThat(response.getRole()).isEqualTo("CLIENT");
    }

    @Test
    void logoutRevokesAnOtherwiseValidTokenImmediately() {
        String token = login();
        assertThat(validate(token).getValid()).isTrue();

        logout(token);

        ValidateResponse afterLogout = validate(token);
        assertThat(afterLogout.getValid()).isFalse();
        assertThat(afterLogout.getUserId()).isEmpty();
        assertThat(afterLogout.getRole()).isEmpty();
    }

    @Test
    void loggingInAgainRightAfterLogoutIssuesATokenThatStillWorks() {
        String revoked = login();
        logout(revoked);

        String reissued = login();

        assertThat(reissued).isNotEqualTo(revoked);
        assertThat(validate(reissued).getValid()).isTrue();
        assertThat(validate(revoked).getValid()).isFalse();
    }

    @Test
    void aSecondLogoutOfTheSameTokenIsHarmless() {
        String token = login();
        logout(token);
        logout(token);

        assertThat(validate(token).getValid()).isFalse();
        assertThat(blacklistKeys()).hasSize(1);
    }

    @Test
    void revokedTokenIsStoredHashedNeverInClearText() {
        String token = login();
        logout(token);

        Set<String> keys = blacklistKeys();
        assertThat(keys).hasSize(1);
        assertThat(keys.iterator().next())
                .matches("bl:[0-9a-f]{64}")
                .doesNotContain(token);
    }

    @Test
    void revokedTokenEntryExpiresWhenTheTokenItselfWouldHave() {
        String token = login();
        logout(token);

        String key = blacklistKeys().iterator().next();

        assertThat(redisTemplate.getExpire(key, TimeUnit.SECONDS))
                .isGreaterThan(EXPIRATION_MS / 1000 - 10)
                .isLessThanOrEqualTo(EXPIRATION_MS / 1000);
    }

    @Test
    void logoutOfAnAlreadyExpiredTokenStoresNothing() {
        String expiredToken = new JwtService(SECRET, -EXPIRATION_MS).generate(userId, "CLIENT", "client@platform.com");

        logout(expiredToken);

        assertThat(blacklistKeys()).isEmpty();
        assertThat(validate(expiredToken).getValid()).isFalse();
    }

    @Test
    void revocationIsVisibleToAnotherReplicaSharingTheSameRedis() {
        String token = login();
        logout(token);

        LettuceConnectionFactory otherConnection = new LettuceConnectionFactory(
                redis.getHost(), redis.getMappedPort(6379));
        otherConnection.afterPropertiesSet();
        otherConnection.start();
        try {
            StringRedisTemplate otherTemplate = new StringRedisTemplate(otherConnection);
            otherTemplate.afterPropertiesSet();
            TokenBlacklistService otherReplica = new TokenBlacklistService(otherTemplate, jwtService);

            assertThat(otherReplica.isBlacklisted(token)).isTrue();
            assertThat(otherReplica.isBlacklisted(jwtService.generate(userId, "ADMIN", "admin@platform.com"))).isFalse();
        } finally {
            otherConnection.stop();
            otherConnection.destroy();
        }
    }

    private String login() {
        return stub.login(LoginRequest.newBuilder()
                .setUsername(EMAIL)
                .setPassword(PASSWORD)
                .build()).getToken();
    }

    private ValidateResponse validate(String token) {
        return stub.validate(ValidateRequest.newBuilder().setToken(token).build());
    }

    private void logout(String token) {
        stub.logout(LogoutRequest.newBuilder().setToken(token).build());
    }

    private Set<String> blacklistKeys() {
        Set<String> keys = redisTemplate.keys("bl:*");
        return keys == null ? Set.of() : keys;
    }
}
