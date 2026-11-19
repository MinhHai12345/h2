package com.h2football.module.workflow.application;

public record WorkflowExecutionContext(
    Long workflowRunId, String workflowType, String stageKey, String requestPayload, int attempt) {}
