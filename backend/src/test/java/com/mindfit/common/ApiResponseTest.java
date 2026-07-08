package com.mindfit.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("ok(data): success=true, code/message는 null, data는 전달값")
    void ok_dataOnly_successTrueAndCodeNull() {
        ApiResponse<String> response = ApiResponse.ok("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("ok(message, data): success=true, code는 null, message/data는 전달값")
    void ok_messageAndData_successTrueAndCodeNull() {
        ApiResponse<String> response = ApiResponse.ok("완료되었습니다", "hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isNull();
        assertThat(response.getMessage()).isEqualTo("완료되었습니다");
        assertThat(response.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("error(String message): success=false, code는 null, message는 전달값, data는 null")
    void error_messageOnly_successFalseAndCodeNull() {
        ApiResponse<Void> response = ApiResponse.error("동적 에러 메시지");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isNull();
        assertThat(response.getMessage()).isEqualTo("동적 에러 메시지");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode): success=false, code는 enum name, message는 ErrorCode의 message")
    void error_errorCode_setsCodeAndMessageFromErrorCode() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INVALID_CREDENTIALS);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getMessage());
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode, message): success=false, code는 enum name, message는 전달값으로 덮어씀")
    void error_errorCodeAndMessage_overridesMessage() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INVALID_INPUT, "field1: 필수입니다, field2: 형식이 올바르지 않습니다");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getMessage()).isEqualTo("field1: 필수입니다, field2: 형식이 올바르지 않습니다");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("success 응답 직렬화 시 code 필드는 JSON에 노출되지 않는다 (NON_NULL)")
    void serialize_successResponse_omitsCodeField() throws Exception {
        ApiResponse<String> response = ApiResponse.ok("hello");
        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).doesNotContain("\"code\"");
    }

    @Test
    @DisplayName("error(ErrorCode) 응답 직렬화 시 success/code/message 필드가 모두 포함된다")
    void serialize_errorResponse_includesSuccessCodeMessage() throws Exception {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.NOT_FOUND);
        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"code\":\"NOT_FOUND\"");
        assertThat(json).contains("\"message\"");
    }
}
