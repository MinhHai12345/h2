package com.h2football.module.workflow.application;

import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorkflowCoordinator {
  private static final Logger log = LoggerFactory.getLogger(WorkflowCoordinator.class);
  private static final int MAX_STAGE_ATTEMPTS = 3;
  private static final Duration LEASE_DURATION = Duration.ofHours(4);

  private final WorkflowDefinitionRegistry definitions;
  private final WorkflowCheckpointService checkpoints;

  public WorkflowCoordinator(
      WorkflowDefinitionRegistry definitions, WorkflowCheckpointService checkpoints) {
    this.definitions = definitions;
    this.checkpoints = checkpoints;
  }

  public WorkflowRun start(String workflowType, String idempotencyKey, String requestPayload) {
    WorkflowDefinition definition = definitions.getRequired(workflowType);
    WorkflowRun run = checkpoints.createOrLoad(workflowType, idempotencyKey, requestPayload);
    return execute(definition, run);
  }

  public WorkflowRun resume(Long workflowRunId) {
    WorkflowRun run = checkpoints.getRun(workflowRunId);
    WorkflowDefinition definition = definitions.getRequired(run.getWorkflowType());
    return execute(definition, run);
  }

  private WorkflowRun execute(WorkflowDefinition definition, WorkflowRun run) {
    if (run.getStatus() == WorkflowRunStatus.SUCCEEDED) {
      return run;
    }
    String owner = UUID.randomUUID().toString();
    checkpoints.acquireLease(run.getId(), owner, LEASE_DURATION);
    try {
      for (int index = 0; index < definition.stages().size(); index++) {
        WorkflowStageDefinition stage = definition.stages().get(index);
        if (checkpoints.isStageCompleted(run.getId(), stage.key())) {
          continue;
        }
        boolean completed = false;
        for (int attempt = 1; attempt <= MAX_STAGE_ATTEMPTS && !completed; attempt++) {
          int actualAttempt = checkpoints.startStage(run.getId(), stage.key(), index);
          try {
            String checkpoint = stage.handler().execute(
                new WorkflowExecutionContext(
                    run.getId(), run.getWorkflowType(), stage.key(), run.getRequestPayload(), actualAttempt));
            checkpoints.completeStage(run.getId(), stage.key(), checkpoint);
            completed = true;
          } catch (Exception exception) {
            checkpoints.failStage(run.getId(), stage.key(), exception.getMessage());
            log.warn(
                "Workflow stage failed: runId={}, stage={}, attempt={}/{}",
                run.getId(), stage.key(), actualAttempt, MAX_STAGE_ATTEMPTS, exception);
            if (attempt == MAX_STAGE_ATTEMPTS) {
              throw exception;
            }
          }
        }
      }
      checkpoints.completeRun(run.getId());
      log.info("Workflow completed: runId={}, type={}", run.getId(), run.getWorkflowType());
    } catch (Exception exception) {
      checkpoints.failRun(run.getId(), exception.getMessage());
      log.error("Workflow failed: runId={}, type={}", run.getId(), run.getWorkflowType(), exception);
    }
    return checkpoints.getRun(run.getId());
  }
}
