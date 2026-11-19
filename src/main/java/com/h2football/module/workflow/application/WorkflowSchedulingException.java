package com.h2football.module.workflow.application;

public class WorkflowSchedulingException extends RuntimeException {
  public WorkflowSchedulingException(String message, Throwable cause) {
    super(message, cause);
  }
}
