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
  private final WorkflowExecutionScheduler scheduler;
  private final Clock clock;

  public WorkflowRecoveryService(
      WorkflowRunRepository runs, WorkflowExecutionScheduler scheduler, Clock clock) {
    this.runs = runs;
    this.scheduler = scheduler;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${app.workflow.recovery-delay-millis:60000}")
  public void recoverPendingAndExpiredRuns() {
    runs.findTop20ByStatusOrderByCreatedAtAsc(WorkflowRunStatus.PENDING)
        .forEach(run -> scheduleRecovery(run.getId(), run.getWorkflowType()));
    runs.findTop20ByStatusAndLeaseExpiresAtBeforeOrderByCreatedAtAsc(
            WorkflowRunStatus.RUNNING, Instant.now(clock))
        .forEach(run -> scheduleRecovery(run.getId(), run.getWorkflowType()));
  }

  private void scheduleRecovery(Long runId, String workflowType) {
    try {
      scheduler.schedule(runId);
      log.info("Scheduled workflow recovery: runId={}, type={}", runId, workflowType);
    } catch (RuntimeException exception) {
      log.warn("Unable to schedule workflow recovery: runId={}", runId, exception);
    }
  }
}
