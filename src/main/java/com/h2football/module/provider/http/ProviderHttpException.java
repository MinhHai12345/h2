package com.h2football.module.provider.http;

import org.springframework.http.HttpStatusCode;

public class ProviderHttpException extends RuntimeException {
  private final HttpStatusCode statusCode;

  public ProviderHttpException(HttpStatusCode statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public HttpStatusCode getStatusCode() {
    return statusCode;
  }
}
