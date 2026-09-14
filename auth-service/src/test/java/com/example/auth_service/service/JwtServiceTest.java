package com.example.auth_service.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private static final String OTHER_SECRET =
            "ffeeddccbbaa99887766554433221100ffeeddccbbaa99887766554433221100";
    private static final long ONE_HOUR = 3_600_000L;

    private final JwtService jwtService = new JwtService(SECRET, ONE_HOUR);

    @Test
    void issuedTokenCarriesTheUserIdAndTheRole() {
        String token = jwtService.generate(42L, "CLIENT");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.getUserId(token)).isEqualTo("42");
        assertThat(jwtService.getRole(token)).isEqualTo("CLIENT");
    }

    @Test
    void twoTokensIssuedInTheSameSecondAreStillDifferent() {
        String first = jwtService.generate(42L, "CLIENT");
        String second = jwtService.generate(42L, "CLIENT");

        assertThat(first).isNotEqualTo(second);
        assertThat(jwtService.isValid(first)).isTrue();
        assertThat(jwtService.isValid(second)).isTrue();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreignToken = new JwtService(OTHER_SECRET, ONE_HOUR).generate(42L, "CLIENT");

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void roleClaimEscalatedWithoutResigningIsRejected() {
        String token = jwtService.generate(42L, "CLIENT");
        String escalated = replaceInPayload(token, "\"role\":\"CLIENT\"", "\"role\":\"ADMIN\"");

        assertThat(escalated).isNotEqualTo(token);
        assertThat(jwtService.isValid(escalated)).isFalse();
        assertThatThrownBy(() -> jwtService.getRole(escalated)).isInstanceOf(Exception.class);
    }

    @Test
    void subjectSwappedToAnotherUserIsRejected() {
        String token = jwtService.generate(42L, "CLIENT");
        String impersonated = replaceInPayload(token, "\"sub\":\"42\"", "\"sub\":\"1\"");

        assertThat(jwtService.isValid(impersonated)).isFalse();
    }

    @Test
    void unsignedAlgNoneTokenIsRejected() {
        String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"1\",\"role\":\"ADMIN\",\"exp\":"
                + (System.currentTimeMillis() / 1000 + 3600) + "}");

        assertThat(jwtService.isValid(header + "." + payload + ".")).isFalse();
    }

    @Test
    void correctlySignedButExpiredTokenIsRejected() {
        String expired = new JwtService(SECRET, -ONE_HOUR).generate(42L, "CLIENT");

        assertThat(jwtService.isValid(expired)).isFalse();
    }

    @Test
    void garbageIsRejectedInsteadOfThrowing() {
        assertThat(jwtService.isValid("not-a-token")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
        assertThat(jwtService.isValid("a.b.c")).isFalse();
    }

    @Test
    void expirationIsReadFromTheTokenItself() {
        long before = System.currentTimeMillis();
        String token = jwtService.generate(42L, "CLIENT");

        assertThat(jwtService.getExpiration(token))
                .isBetween(before + ONE_HOUR - 5_000, System.currentTimeMillis() + ONE_HOUR);
    }

    @Test
    void expirationOfAnUnreadableTokenIsZeroSoNoTtlCanBeDerivedFromIt() {
        assertThat(jwtService.getExpiration("not-a-token")).isZero();
        assertThat(jwtService.getExpiration(new JwtService(OTHER_SECRET, ONE_HOUR).generate(1L, "ADMIN")))
                .isZero();
    }

    @Test
    void payloadIsPlainReadableBase64SoTheTamperingTestsReallyTamper() {
        String token = jwtService.generate(7L, "OWNER_EVENT");
        String payload = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).contains("\"sub\":\"7\"", "\"role\":\"OWNER_EVENT\"");
    }

    private static String replaceInPayload(String token, String from, String to) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains(from);
        return parts[0] + "." + base64Url(payload.replace(from, to)) + "." + parts[2];
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
