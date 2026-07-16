package com.mindfit.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    /**
     * Spring 컨텍스트 내 생성자 주입 (@Value).
     * 테스트에서는 직접 new JwtTokenProvider(secret, access, refresh) 로 생성 가능.
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    /**
     * 액세스 토큰 생성.
     *
     * @param userId 사용자 ID (sub 클레임)
     * @param role   사용자 역할 (ROLE_CLIENT / ROLE_COUNSELOR / ROLE_ADMIN)
     */
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpiry))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 리프레시 토큰 생성.
     *
     * @param userId 사용자 ID (sub 클레임)
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpiry))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 검증 결과. 만료(EXPIRED)와 그 외 무효(INVALID)를 구분한다.
     */
    public enum TokenStatus {
        VALID, EXPIRED, INVALID
    }

    /**
     * 토큰 유효성 검증.
     *
     * @return 유효하면 true, 그 외(만료·변조·형식 오류 등) false
     */
    public boolean validateToken(String token) {
        return validate(token) == TokenStatus.VALID;
    }

    /**
     * 토큰을 검증하고 만료/무효를 구분한 상태를 반환한다.
     *
     * @return VALID(유효) / EXPIRED(만료) / INVALID(변조·형식 오류 등)
     */
    public TokenStatus validate(String token) {
        try {
            parseClaims(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            return TokenStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return TokenStatus.INVALID;
        }
    }

    /**
     * 토큰에서 userId(sub 클레임) 추출.
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 role 클레임 추출.
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    // ── private ──

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
