package com.mindfit.auth;

import com.mindfit.auth.dto.LoginRequest;
import com.mindfit.auth.dto.SignupRequest;
import com.mindfit.auth.dto.TokenResponse;
import com.mindfit.common.BusinessException;
import com.mindfit.common.ErrorCode;
import com.mindfit.user.User;
import com.mindfit.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .role(request.getRole())
                .emailVerified(false)
                .emailVerifyToken(UUID.randomUUID().toString())
                .build();
        user = userRepository.save(user);

        return generateTokenResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return generateTokenResponse(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getRole().name());
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.updateRefreshToken(null);
            userRepository.save(user);
        });
    }

    private TokenResponse generateTokenResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }
}
