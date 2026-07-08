package com.mindfit.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleBusinessException: BusinessException의 ErrorCode로 status/code/message를 채운다")
    void handleBusinessException_returnsErrorCodeStatusAndBody() {
        BusinessException exception = new BusinessException(ErrorCode.INVALID_CREDENTIALS);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getStatus());
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("handleValidationException: 400 + code=INVALID_INPUT + 필드 에러 메시지 조합")
    void handleValidationException_returns400WithInvalidInputCode() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("target", "email", "이메일 형식이 올바르지 않습니다");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().getMessage()).isEqualTo("이메일 형식이 올바르지 않습니다");
    }

    @Test
    @DisplayName("handleAccessDeniedException: 403 + code=ACCESS_DENIED")
    void handleAccessDeniedException_returns403WithAccessDeniedCode() {
        AccessDeniedException exception = new AccessDeniedException("접근 불가");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("handleException: 500 + code=INTERNAL_ERROR")
    void handleException_returns500WithInternalErrorCode() {
        Exception exception = new RuntimeException("예상치 못한 오류");

        ResponseEntity<ApiResponse<Void>> response = handler.handleException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
