package com.h2football.module.workflow.application;

public class WorkflowStageRejectedException extends RuntimeException {
  public WorkflowStageRejectedException(String message) {
    super(message);
  }
}
