package com.h2football.module.workflow.application;

public record WorkflowStageDefinition(String key, WorkflowStageHandler handler) {
  public WorkflowStageDefinition {
    if (key == null || key.isBlank() || handler == null) {
      throw new IllegalArgumentException("Stage key and handler are required");
    }
  }
}
