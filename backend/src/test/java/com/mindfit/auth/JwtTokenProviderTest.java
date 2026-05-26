package com.mindfit.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET_KEY =
            "test-secret-key-mindfit-2024-very-long-secret-key-for-hs256-testing";
    private static final long ACCESS_TOKEN_EXPIRY  = 1_800_000L; // 30분
    private static final long REFRESH_TOKEN_EXPIRY = 604_800_000L; // 7일

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET_KEY, ACCESS_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY);
    }

    // ──────────────── generateAccessToken ────────────────

    @Test
    @DisplayName("generateAccessToken: 유효한 입력으로 빈 문자열이 아닌 토큰 반환")
    void generateAccessToken_validInput_returnsNonBlankToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_CLIENT");

        assertThat(token).isNotBlank();
    }

    // ──────────────── generateRefreshToken ────────────────

    @Test
    @DisplayName("generateRefreshToken: 유효한 입력으로 빈 문자열이 아닌 토큰 반환")
    void generateRefreshToken_validInput_returnsNonBlankToken() {
        String token = jwtTokenProvider.generateRefreshToken(1L);

        assertThat(token).isNotBlank();
    }

    // ──────────────── validateToken ────────────────

    @Test
    @DisplayName("validateToken: 정상 토큰이면 true 반환")
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_CLIENT");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: 변조된 토큰이면 false 반환")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_CLIENT");
        String tampered = token + "tampered";

        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken: 만료된 토큰이면 false 반환 (1ms expiry)")
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        JwtTokenProvider shortLivedProvider =
                new JwtTokenProvider(SECRET_KEY, 1L, 1L); // 1ms expiry
        String token = shortLivedProvider.generateAccessToken(42L, "ROLE_COUNSELOR");

        Thread.sleep(10); // 토큰 만료 대기

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    // ──────────────── getUserIdFromToken ────────────────

    @Test
    @DisplayName("getUserIdFromToken: 정상 토큰에서 올바른 userId 반환")
    void getUserIdFromToken_validToken_returnsCorrectUserId() {
        Long expectedUserId = 42L;
        String token = jwtTokenProvider.generateAccessToken(expectedUserId, "ROLE_CLIENT");

        Long actualUserId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    // ──────────────── getRoleFromToken ────────────────

    @Test
    @DisplayName("getRoleFromToken: 정상 토큰에서 올바른 role 반환")
    void getRoleFromToken_validToken_returnsCorrectRole() {
        String expectedRole = "ROLE_COUNSELOR";
        String token = jwtTokenProvider.generateAccessToken(1L, expectedRole);

        String actualRole = jwtTokenProvider.getRoleFromToken(token);

        assertThat(actualRole).isEqualTo(expectedRole);
    }
}
