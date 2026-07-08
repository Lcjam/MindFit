package com.mindfit.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ──────────────── doFilterInternal ────────────────

    @Test
    @DisplayName("doFilterInternal: 유효한 Bearer 토큰이면 SecurityContext에 인증 설정 → 보호 엔드포인트 200")
    void doFilterInternal_validBearerToken_setsAuthentication() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_CLIENT");

        mockMvc.perform(get("/api/v1/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("doFilterInternal: Authorization 헤더 없으면 인증을 건너뜀 → permitAll 엔드포인트는 200")
    void doFilterInternal_missingAuthHeader_skipsAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("doFilterInternal: 유효하지 않은 토큰이면 인증을 건너뜀 → 보호 엔드포인트 401")
    void doFilterInternal_invalidToken_skipsAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/protected")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }
}
