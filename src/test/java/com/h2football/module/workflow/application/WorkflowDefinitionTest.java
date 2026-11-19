package com.h2football.module.workflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowDefinitionTest {
  @Test
  void preservesDeclaredStageOrder() {
    WorkflowStageHandler handler = context -> "{}";
    WorkflowDefinition definition =
        new WorkflowDefinition(
            "TEST",
            List.of(
                new WorkflowStageDefinition("INGESTION", handler),
                new WorkflowStageDefinition("ANALYTICS", handler)));

    assertEquals(List.of("INGESTION", "ANALYTICS"),
        definition.stages().stream().map(WorkflowStageDefinition::key).toList());
  }

  @Test
  void rejectsDuplicateStageKeys() {
    WorkflowStageHandler handler = context -> "{}";
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new WorkflowDefinition(
                "TEST",
                List.of(
                    new WorkflowStageDefinition("INGESTION", handler),
                    new WorkflowStageDefinition("INGESTION", handler))));
  }
}
