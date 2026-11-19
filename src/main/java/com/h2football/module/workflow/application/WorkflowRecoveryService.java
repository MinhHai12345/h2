package com.h2football.module.workflow.application;

import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import com.h2football.module.workflow.domain.repository.WorkflowRunRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WorkflowRecoveryService {
  private static final Logger log = LoggerFactory.getLogger(WorkflowRecoveryService.class);

  private final WorkflowRunRepository runs;
  private final WorkflowCoordinator coordinator;
  private final Clock clock;

  public WorkflowRecoveryService(
      WorkflowRunRepository runs, WorkflowCoordinator coordinator, Clock clock) {
    this.runs = runs;
    this.coordinator = coordinator;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${app.workflow.recovery-delay-millis:60000}")
  public void recoverExpiredRuns() {
    var expiredRuns =
        runs.findTop20ByStatusAndLeaseExpiresAtBeforeOrderByCreatedAtAsc(
            WorkflowRunStatus.RUNNING, Instant.now(clock));
    for (var run : expiredRuns) {
      try {
        log.info(
            "Recovering expired workflow lease: runId={}, type={}",
            run.getId(),
            run.getWorkflowType());
        coordinator.resume(run.getId());
      } catch (IllegalStateException exception) {
        log.debug(
            "Workflow recovery skipped for runId={}: {}", run.getId(), exception.getMessage());
      } catch (RuntimeException exception) {
        log.warn("Workflow recovery failed for runId={}", run.getId(), exception);
      }
    }
  }
}
