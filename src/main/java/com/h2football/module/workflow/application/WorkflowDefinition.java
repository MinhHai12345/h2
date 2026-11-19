package com.h2football.module.workflow.application;

import java.util.HashSet;
import java.util.List;

public record WorkflowDefinition(String key, List<WorkflowStageDefinition> stages) {
  public WorkflowDefinition {
    if (key == null || key.isBlank() || stages == null || stages.isEmpty()) {
      throw new IllegalArgumentException("Workflow key and at least one stage are required");
    }
    stages = List.copyOf(stages);
    if (new HashSet<>(stages.stream().map(WorkflowStageDefinition::key).toList()).size()
        != stages.size()) {
      throw new IllegalArgumentException("Workflow stage keys must be unique");
    }
  }
}
