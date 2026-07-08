package com.mindfit.auth;

import com.mindfit.auth.dto.LoginRequest;
import com.mindfit.auth.dto.SignupRequest;
import com.mindfit.auth.dto.TokenResponse;
import com.mindfit.common.BusinessException;
import com.mindfit.common.ErrorCode;
import com.mindfit.user.Role;
import com.mindfit.user.User;
import com.mindfit.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User buildUser(Long id, String email, String encodedPassword, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .password(encodedPassword)
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(role)
                .emailVerified(false)
                .build();
    }

    // ──────────────── signup ────────────────

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("새 이메일이면 사용자를 저장하고 TokenResponse를 반환한다")
        void signup_newEmail_returnsTokenResponse() {
            SignupRequest request = SignupRequest.builder()
                    .email("new@mindfit.com")
                    .password("password123")
                    .name("홍길동")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .role(Role.ROLE_CLIENT)
                    .build();

            given(userRepository.existsByEmail("new@mindfit.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encoded-password");
            given(userRepository.save(any(User.class)))
                    .willReturn(buildUser(1L, "new@mindfit.com", "encoded-password", Role.ROLE_CLIENT));
            given(jwtTokenProvider.generateAccessToken(1L, "ROLE_CLIENT")).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");

            TokenResponse response = authService.signup(request);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getRole()).isEqualTo("ROLE_CLIENT");
            // signup()에서 1회 + generateTokenResponse()에서 refreshToken 저장 1회 = 2회
            verify(userRepository, times(2)).save(any(User.class));
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 DUPLICATE_EMAIL 예외를 던진다")
        void signup_duplicateEmail_throwsDuplicateEmailException() {
            SignupRequest request = SignupRequest.builder()
                    .email("dup@mindfit.com")
                    .password("password123")
                    .name("홍길동")
                    .role(Role.ROLE_CLIENT)
                    .build();

            given(userRepository.existsByEmail("dup@mindfit.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("ROLE_COUNSELOR로 가입하면 사용자를 저장하고 TokenResponse를 반환한다")
        void signup_roleCounselor_returnsTokenResponse() {
            SignupRequest request = SignupRequest.builder()
                    .email("counselor@mindfit.com")
                    .password("password123")
                    .name("홍길동")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .role(Role.ROLE_COUNSELOR)
                    .build();

            given(userRepository.existsByEmail("counselor@mindfit.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encoded-password");
            given(userRepository.save(any(User.class)))
                    .willReturn(buildUser(5L, "counselor@mindfit.com", "encoded-password", Role.ROLE_COUNSELOR));
            given(jwtTokenProvider.generateAccessToken(5L, "ROLE_COUNSELOR")).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(5L)).willReturn("refresh-token");

            TokenResponse response = authService.signup(request);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getUserId()).isEqualTo(5L);
            assertThat(response.getRole()).isEqualTo("ROLE_COUNSELOR");
        }

        @Test
        @DisplayName("ROLE_ADMIN으로 자가가입을 시도하면 FORBIDDEN_ROLE 예외를 던진다")
        void signup_roleAdmin_throwsForbiddenRole() {
            SignupRequest request = SignupRequest.builder()
                    .email("admin@mindfit.com")
                    .password("password123")
                    .name("홍길동")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .role(Role.ROLE_ADMIN)
                    .build();

            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_ROLE);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ──────────────── login ────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("이메일·비밀번호가 일치하면 TokenResponse를 반환한다")
        void login_validCredentials_returnsTokenResponse() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@mindfit.com")
                    .password("password123")
                    .build();

            User user = buildUser(2L, "user@mindfit.com", "encoded-password", Role.ROLE_COUNSELOR);
            given(userRepository.findByEmail("user@mindfit.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
            given(jwtTokenProvider.generateAccessToken(2L, "ROLE_COUNSELOR")).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(2L)).willReturn("refresh-token");

            TokenResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getUserId()).isEqualTo(2L);
            assertThat(response.getRole()).isEqualTo("ROLE_COUNSELOR");
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 INVALID_CREDENTIALS 예외를 던진다")
        void login_emailNotFound_throwsInvalidCredentials() {
            LoginRequest request = LoginRequest.builder()
                    .email("missing@mindfit.com")
                    .password("password123")
                    .build();

            given(userRepository.findByEmail("missing@mindfit.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외를 던진다")
        void login_wrongPassword_throwsInvalidCredentials() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@mindfit.com")
                    .password("wrong-password")
                    .build();

            User user = buildUser(2L, "user@mindfit.com", "encoded-password", Role.ROLE_CLIENT);
            given(userRepository.findByEmail("user@mindfit.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // ──────────────── refresh ────────────────

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("유효한 토큰이고 저장된 토큰과 일치하면 새 access·refresh 토큰을 회전 발급한다")
        void refresh_validToken_rotatesBothTokens() {
            String storedRefreshToken = "stored-refresh-token";
            User user = buildUser(3L, "user@mindfit.com", "encoded-password", Role.ROLE_CLIENT);
            user.updateRefreshToken(storedRefreshToken);

            given(jwtTokenProvider.validateToken(storedRefreshToken)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(storedRefreshToken)).willReturn(3L);
            given(userRepository.findById(3L)).willReturn(Optional.of(user));
            given(jwtTokenProvider.generateAccessToken(3L, "ROLE_CLIENT")).willReturn("new-access-token");
            given(jwtTokenProvider.generateRefreshToken(3L)).willReturn("new-refresh-token");

            TokenResponse response = authService.refresh(storedRefreshToken);

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            // refresh token rotation: 입력 토큰을 재사용하지 않고 새 토큰을 발급한다
            assertThat(response.getRefreshToken()).isNotEqualTo(storedRefreshToken);
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.getUserId()).isEqualTo(3L);
            assertThat(response.getRole()).isEqualTo("ROLE_CLIENT");
            // 회전된 새 토큰으로 사용자 상태가 갱신되어 저장된다
            assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("토큰이 유효하지 않으면 INVALID_TOKEN 예외를 던진다")
        void refresh_invalidToken_throwsInvalidToken() {
            given(jwtTokenProvider.validateToken("bad-token")).willReturn(false);

            assertThatThrownBy(() -> authService.refresh("bad-token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("저장된 refreshToken과 일치하지 않으면 INVALID_TOKEN 예외를 던진다")
        void refresh_mismatchedStoredToken_throwsInvalidToken() {
            String incomingToken = "incoming-refresh-token";
            User user = buildUser(3L, "user@mindfit.com", "encoded-password", Role.ROLE_CLIENT);
            user.updateRefreshToken("different-stored-token");

            given(jwtTokenProvider.validateToken(incomingToken)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(incomingToken)).willReturn(3L);
            given(userRepository.findById(3L)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.refresh(incomingToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }

    // ──────────────── logout ────────────────

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("존재하는 사용자면 refreshToken을 null로 초기화한다")
        void logout_existingUser_clearsRefreshToken() {
            User user = buildUser(4L, "user@mindfit.com", "encoded-password", Role.ROLE_CLIENT);
            user.updateRefreshToken("some-token");
            given(userRepository.findById(4L)).willReturn(Optional.of(user));

            authService.logout(4L);

            assertThat(user.getRefreshToken()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 예외 없이 정상 종료한다")
        void logout_nonExistingUser_doesNotThrow() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            authService.logout(99L);

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
