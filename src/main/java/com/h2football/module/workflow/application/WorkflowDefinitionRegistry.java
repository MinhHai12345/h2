package com.h2football.module.workflow.application;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WorkflowDefinitionRegistry {
  private final Map<String, WorkflowDefinition> definitions;

  public WorkflowDefinitionRegistry(java.util.List<WorkflowDefinition> definitions) {
    this.definitions =
        definitions.stream()
            .collect(Collectors.toUnmodifiableMap(WorkflowDefinition::key, Function.identity()));
  }

  public WorkflowDefinition getRequired(String key) {
    WorkflowDefinition definition = definitions.get(key);
    if (definition == null) {
      throw new IllegalArgumentException("Unknown workflow type: " + key);
    }
    return definition;
  }
}
