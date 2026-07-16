package com.mindfit.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindfit.auth.dto.LoginRequest;
import com.mindfit.auth.dto.RefreshRequest;
import com.mindfit.auth.dto.SignupRequest;
import com.mindfit.auth.dto.TokenResponse;
import com.mindfit.common.BusinessException;
import com.mindfit.common.ErrorCode;
import com.mindfit.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class, RestAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-mindfit-2024-very-long-secret-key-for-hs256-testing",
        "jwt.access-token-expiry=1800000",
        "jwt.refresh-token-expiry=604800000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthService authService;

    private TokenResponse dummyToken() {
        return TokenResponse.builder()
                .accessToken("access.token.here")
                .refreshToken("refresh.token.here")
                .userId(1L)
                .role("ROLE_CLIENT")
                .build();
    }

    // ──────────────── signup ────────────────

    @Test
    @DisplayName("signup: 유효한 요청이면 authService.signup 호출 후 201 + success=true 반환")
    void signup_validRequest_returns201() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .email("client@mindfit.com")
                .password("password123")
                .name("홍길동")
                .role(Role.ROLE_CLIENT)
                .build();
        BDDMockito.given(authService.signup(any(SignupRequest.class))).willReturn(dummyToken());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.token.here"));

        verify(authService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("signup: 이메일 형식이 올바르지 않으면 400 반환")
    void signup_invalidEmail_returns400() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .email("not-an-email")
                .password("password123")
                .name("홍길동")
                .role(Role.ROLE_CLIENT)
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("signup: 비밀번호가 공백이면 400 반환")
    void signup_blankPassword_returns400() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .email("client@mindfit.com")
                .password("")
                .name("홍길동")
                .role(Role.ROLE_CLIENT)
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("signup: 중복 이메일이면 authService가 BusinessException(DUPLICATE_EMAIL) → 409 반환")
    void signup_duplicateEmail_returns409() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .email("client@mindfit.com")
                .password("password123")
                .name("홍길동")
                .role(Role.ROLE_CLIENT)
                .build();
        BDDMockito.given(authService.signup(any(SignupRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────── login ────────────────

    @Test
    @DisplayName("login: 유효한 요청이면 200 + data.accessToken 반환")
    void login_validRequest_returns200() throws Exception {
        LoginRequest request = new LoginRequest("client@mindfit.com", "password123");
        BDDMockito.given(authService.login(any(LoginRequest.class))).willReturn(dummyToken());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.token.here"));
    }

    @Test
    @DisplayName("login: 자격 증명이 잘못되면 authService가 BusinessException(INVALID_CREDENTIALS) → 401 반환")
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("client@mindfit.com", "wrongpassword");
        BDDMockito.given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────── refresh ────────────────

    @Test
    @DisplayName("refresh: 유효한 refreshToken이면 200 + data.accessToken 반환")
    void refresh_validToken_returns200() throws Exception {
        RefreshRequest request = new RefreshRequest("valid.refresh.token");
        BDDMockito.given(authService.refresh(any(String.class))).willReturn(dummyToken());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.token.here"));
    }

    @Test
    @DisplayName("refresh: 토큰이 유효하지 않으면 authService가 BusinessException(INVALID_TOKEN) → 401 반환")
    void refresh_invalidToken_returns401() throws Exception {
        RefreshRequest request = new RefreshRequest("invalid.refresh.token");
        BDDMockito.given(authService.refresh(any(String.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────── logout ────────────────

    @Test
    @DisplayName("logout: 유효한 Bearer 토큰이면 200 + success=true 반환")
    void logout_authenticated_returns200() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_CLIENT");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(1L);
    }

    @Test
    @DisplayName("logout: Authorization 헤더가 없으면 401 반환")
    void logout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
