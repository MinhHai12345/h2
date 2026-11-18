package com.h2football.shared.exception;

import com.h2football.shared.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturnSafeInternalErrorResponse() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"), ZoneOffset.UTC);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(clock);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/diagnostics");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new HttpRequestMethodNotSupportedException("POST"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("HttpRequestMethodNotSupportedException");
        assertThat(response.getBody().timestamp()).isEqualTo(clock.instant());
        assertThat(response.getBody().path()).isEqualTo("/api/diagnostics");
    }
}
