package com.h2football.shared.exception;

import com.h2football.module.analytics.application.MatchAnalyticsNotFoundException;
import com.h2football.module.identity.application.ExternalIdentityConflictException;
import com.h2football.module.masterdata.application.DuplicateMasterDataException;
import com.h2football.module.masterdata.application.MasterDataNotFoundException;
import com.h2football.module.match.application.MatchNotFoundException;
import com.h2football.module.operations.application.CronJobNotFoundException;
import com.h2football.module.operations.application.CronJobOperationException;
import com.h2football.shared.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION", exception.getMessage(), request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST_BODY",
        "Request body is missing or malformed",
        request);
  }

  @ExceptionHandler({
    MasterDataNotFoundException.class,
    MatchNotFoundException.class,
    MatchAnalyticsNotFoundException.class,
    CronJobNotFoundException.class
  })
  public ResponseEntity<ApiErrorResponse> handleNotFound(
      RuntimeException exception, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
  }

  @ExceptionHandler({
    DuplicateMasterDataException.class,
    ExternalIdentityConflictException.class,
    DataIntegrityViolationException.class
  })
  public ResponseEntity<ApiErrorResponse> handleConflict(
      Exception exception, HttpServletRequest request) {
    return build(
        HttpStatus.CONFLICT,
        "RESOURCE_CONFLICT",
        "The resource conflicts with existing data",
        request);
  }

  @ExceptionHandler(CronJobOperationException.class)
  public ResponseEntity<ApiErrorResponse> handleCronJobOperation(
      CronJobOperationException exception, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, "CRON_JOB_OPERATION_FAILED", exception.getMessage(), request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", exception.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        request);
  }

  private ResponseEntity<ApiErrorResponse> build(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(clock), status.value(), code, message, request.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }
}
