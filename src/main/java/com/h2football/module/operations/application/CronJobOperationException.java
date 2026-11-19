package com.h2football.module.operations.application;

public class CronJobOperationException extends RuntimeException {
  public CronJobOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  public CronJobOperationException(String message) {
    super(message);
  }
}
