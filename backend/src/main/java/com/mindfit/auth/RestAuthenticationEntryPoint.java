package com.mindfit.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindfit.common.ApiResponse;
import com.mindfit.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증 실패(401) 시 계약(ErrorResponse: success/code/message)에 맞는 JSON 본문을 직접 반환한다.
 *
 * <p>JwtAuthFilter가 request attribute({@link #AUTH_ERROR_CODE_ATTR})로 전달한 ErrorCode를 읽어
 * 만료(EXPIRED_TOKEN)와 무효(INVALID_TOKEN)를 구분한다. attribute가 없으면(완전 무토큰 등)
 * INVALID_TOKEN을 기본으로 사용한다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** JwtAuthFilter가 세팅하는 인증 오류 코드 attribute 키. */
    public static final String AUTH_ERROR_CODE_ATTR = "authErrorCode";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorCode errorCode = resolveErrorCode(request);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        Object attr = request.getAttribute(AUTH_ERROR_CODE_ATTR);
        if (attr instanceof ErrorCode errorCode) {
            return errorCode;
        }
        return ErrorCode.INVALID_TOKEN;
    }
}
