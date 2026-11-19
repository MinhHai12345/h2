package com.h2football.module.workflow.application;

@FunctionalInterface
public interface WorkflowStageHandler {
  String execute(WorkflowExecutionContext context) throws Exception;
}
