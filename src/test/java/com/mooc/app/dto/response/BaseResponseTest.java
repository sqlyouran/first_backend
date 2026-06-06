package com.mooc.app.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializes_requestId_as_snake_case() throws Exception {
        // A concrete subclass for testing
        BaseResponse response = new BaseResponse("test-request-id") {};
        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"request_id\"");
        assertThat(json).contains("\"test-request-id\"");
        assertThat(json).doesNotContain("\"requestId\"");
    }
}
