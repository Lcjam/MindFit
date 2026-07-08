package com.mindfit.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    // ──────────────── B4) logout 인증 요구 ────────────────

    @Test
    @DisplayName("logout: 토큰 없이 POST /api/v1/auth/logout → Security 레이어에서 401 거부")
    void logout_withoutToken_returns401AtSecurityLayer() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                // logout이 permitAll이면 요청이 컨트롤러까지 도달해
                // GlobalExceptionHandler가 ApiResponse(JSON, "success" 필드 포함)를 내려준다.
                // authenticated로 분리되면 Security 진입점이 sendError(401)로 먼저 차단하므로
                // ApiResponse JSON 본문이 존재하지 않아야 한다.
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body)
                            .as("Security 레이어에서 차단되어 컨트롤러 ApiResponse 본문이 없어야 한다")
                            .doesNotContain("\"success\"");
                });
    }

    @Test
    @DisplayName("login: 토큰 없이 POST /api/v1/auth/login → permitAll이라 Security 401이 아니다")
    void login_withoutToken_notBlockedBySecurity() throws Exception {
        // 본문 없는 요청이라 parse/validation 단계(4xx)까지 도달할 수 있으나,
        // permitAll 경로이므로 Security 레이어의 401은 아니어야 한다.
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    // ──────────────── B3) CORS allowedOrigins 환경설정 주입 ────────────────

    @Test
    @DisplayName("CORS: allowedOrigins가 환경설정(app.cors.allowed-origins) 값을 반영한다")
    void corsConfig_reflectsConfiguredAllowedOrigins() {
        assertThat(corsConfigurationSource).isInstanceOf(UrlBasedCorsConfigurationSource.class);

        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) corsConfigurationSource)
                .getCorsConfigurations()
                .get("/**");

        assertThat(config).isNotNull();
        // 테스트 프로파일(application.yml)의 app.cors.allowed-origins 값이 주입되어야 한다.
        assertThat(config.getAllowedOrigins()).contains("http://localhost:5173");
    }
}
