package com.h2football.module.workflow.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowCoordinatorTest {
  @Mock private WorkflowDefinitionRegistry definitions;
  @Mock private WorkflowCheckpointService checkpoints;
  @Mock private WorkflowRun run;

  @Test
  void shouldNotRetryAStageRejectedByDataReadinessGate() throws Exception {
    WorkflowStageHandler rejectedHandler =
        context -> {
          throw new WorkflowStageRejectedException("Data quality gate rejected the season");
        };
    WorkflowDefinition definition =
        new WorkflowDefinition(
            "SEASON_PIPELINE",
            List.of(new WorkflowStageDefinition("DATA_READINESS", rejectedHandler)));
    when(checkpoints.getRun(7L)).thenReturn(run);
    when(run.getId()).thenReturn(7L);
    when(run.getWorkflowType()).thenReturn("SEASON_PIPELINE");
    when(run.getStatus()).thenReturn(WorkflowRunStatus.PENDING);
    when(run.getRequestPayload()).thenReturn("{}");
    when(definitions.getRequired("SEASON_PIPELINE")).thenReturn(definition);
    when(checkpoints.isStageCompleted(7L, "DATA_READINESS")).thenReturn(false);
    when(checkpoints.startStage(eq(7L), anyString(), eq("DATA_READINESS"), eq(0)))
        .thenReturn(1);
    when(checkpoints.failRun(eq(7L), anyString(), eq("Data quality gate rejected the season")))
        .thenReturn(true);

    WorkflowCoordinator coordinator = new WorkflowCoordinator(definitions, checkpoints);
    coordinator.resume(7L);

    verify(checkpoints).startStage(eq(7L), anyString(), eq("DATA_READINESS"), eq(0));
    verify(checkpoints)
        .failStage(
            eq(7L),
            anyString(),
            eq("DATA_READINESS"),
            eq("Data quality gate rejected the season"));
    verify(checkpoints, never()).completeRun(eq(7L), any());
  }
}
