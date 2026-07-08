package com.mindfit.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String email) {
        return User.builder()
                .email(email)
                .password("encodedPassword123")
                .name("테스트유저")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.ROLE_CLIENT)
                .emailVerifyToken("token-" + email)
                .build();
    }

    @Test
    @DisplayName("새 유저 저장 시 emailVerified=false, createdAt 자동 세팅")
    void save_newUser_persistsWithEmailUnverifiedAndCreatedAt() {
        User user = buildUser("test@mindfit.com");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하는 이메일로 조회 시 유저 반환")
    void findByEmail_existingEmail_returnsUser() {
        userRepository.save(buildUser("find@mindfit.com"));

        Optional<User> result = userRepository.findByEmail("find@mindfit.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("find@mindfit.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 빈 Optional 반환")
    void findByEmail_nonExistingEmail_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("nobody@mindfit.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하는 이메일로 existsByEmail 조회 시 true 반환")
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(buildUser("exists@mindfit.com"));

        boolean result = userRepository.existsByEmail("exists@mindfit.com");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 existsByEmail 조회 시 false 반환")
    void existsByEmail_nonExistingEmail_returnsFalse() {
        boolean result = userRepository.existsByEmail("notexists@mindfit.com");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효한 이메일 인증 토큰으로 조회 시 유저 반환")
    void findByEmailVerifyToken_validToken_returnsUser() {
        User user = buildUser("token@mindfit.com");
        userRepository.save(user);

        Optional<User> result = userRepository.findByEmailVerifyToken("token-token@mindfit.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmailVerifyToken()).isEqualTo("token-token@mindfit.com");
    }

    @Test
    @DisplayName("유효하지 않은 이메일 인증 토큰으로 조회 시 빈 Optional 반환")
    void findByEmailVerifyToken_invalidToken_returnsEmpty() {
        Optional<User> result = userRepository.findByEmailVerifyToken("invalid-token");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("중복 이메일 저장 시 DataIntegrityViolationException 발생")
    void save_duplicateEmail_throwsConstraintViolation() {
        userRepository.save(buildUser("dup@mindfit.com"));
        userRepository.flush();

        User duplicate = buildUser("dup@mindfit.com");

        assertThatThrownBy(() -> {
            userRepository.save(duplicate);
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
