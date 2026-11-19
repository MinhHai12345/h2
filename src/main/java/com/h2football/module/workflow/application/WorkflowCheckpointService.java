package com.h2football.module.workflow.application;

import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowStageExecution;
import com.h2football.module.workflow.domain.model.WorkflowStageStatus;
import com.h2football.module.workflow.domain.repository.WorkflowRunRepository;
import com.h2football.module.workflow.domain.repository.WorkflowStageExecutionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowCheckpointService {
  private final WorkflowRunRepository runs;
  private final WorkflowStageExecutionRepository stages;
  private final Clock clock;

  public WorkflowCheckpointService(
      WorkflowRunRepository runs, WorkflowStageExecutionRepository stages, Clock clock) {
    this.runs = runs;
    this.stages = stages;
    this.clock = clock;
  }

  @Transactional
  public WorkflowRun createOrLoad(String type, String key, String payload) {
    runs.insertIfAbsent(type, key, payload);
    WorkflowRun run =
        runs.findByWorkflowTypeAndIdempotencyKey(type, key)
            .orElseThrow(
                () -> new IllegalStateException("Workflow run could not be created or loaded"));
    if (!run.getRequestPayload().equals(payload)) {
      throw new IllegalArgumentException(
          "The idempotency key is already associated with a different workflow request");
    }
    return run;
  }

  @Transactional
  public void acquireLease(Long runId, String owner, Duration duration) {
    WorkflowRun run = lockRun(runId);
    Instant now = Instant.now(clock);
    run.claim(owner, now.plus(duration), now);
  }

  @Transactional(readOnly = true)
  public boolean isStageCompleted(Long runId, String stageKey) {
    return stages
        .findByWorkflowRun_IdAndStageKey(runId, stageKey)
        .map(stage -> stage.getStatus() == WorkflowStageStatus.SUCCEEDED)
        .orElse(false);
  }

  @Transactional
  public int startStage(Long runId, String owner, String stageKey, int order) {
    WorkflowRun run = lockRun(runId);
    ensureLeaseOwner(run, owner);
    WorkflowStageExecution stage =
        stages
            .findByWorkflowRun_IdAndStageKey(runId, stageKey)
            .orElseGet(() -> new WorkflowStageExecution(run, stageKey, order));
    stage.start(Instant.now(clock));
    run.selectStage(stageKey);
    return stages.save(stage).getAttemptCount();
  }

  @Transactional
  public void completeStage(Long runId, String owner, String stageKey, String checkpointPayload) {
    ensureLeaseOwner(lockRun(runId), owner);
    WorkflowStageExecution stage = findStage(runId, stageKey);
    stage.succeed(checkpointPayload, Instant.now(clock));
    stages.save(stage);
  }

  @Transactional
  public void failStage(Long runId, String owner, String stageKey, String message) {
    ensureLeaseOwner(lockRun(runId), owner);
    WorkflowStageExecution stage = findStage(runId, stageKey);
    stage.fail(message);
    stages.save(stage);
  }

  @Transactional
  public void completeRun(Long runId, String owner) {
    WorkflowRun run = lockRun(runId);
    ensureLeaseOwner(run, owner);
    run.complete(Instant.now(clock));
  }

  @Transactional
  public boolean failRun(Long runId, String owner, String message) {
    WorkflowRun run = lockRun(runId);
    if (!ownsLease(run, owner, Instant.now(clock))) {
      return false;
    }
    run.fail(message);
    return true;
  }

  @Transactional(readOnly = true)
  public WorkflowRun getRun(Long runId) {
    return runs.findById(runId)
        .orElseThrow(() -> new IllegalArgumentException("Workflow run not found: " + runId));
  }

  @Transactional(readOnly = true)
  public List<WorkflowStageExecution> getStages(Long runId) {
    return stages.findByWorkflowRun_IdOrderByStageOrderAsc(runId);
  }

  private WorkflowRun lockRun(Long runId) {
    return runs.findByIdForUpdate(runId)
        .orElseThrow(() -> new IllegalArgumentException("Workflow run not found: " + runId));
  }

  private WorkflowStageExecution findStage(Long runId, String stageKey) {
    return stages
        .findByWorkflowRun_IdAndStageKey(runId, stageKey)
        .orElseThrow(() -> new IllegalArgumentException("Workflow stage not found: " + stageKey));
  }

  private void ensureLeaseOwner(WorkflowRun run, String owner) {
    if (!ownsLease(run, owner, Instant.now(clock))) {
      throw new IllegalStateException("Workflow lease is no longer owned by this worker");
    }
  }

  private boolean ownsLease(WorkflowRun run, String owner, Instant now) {
    return owner != null
        && owner.equals(run.getLeaseOwner())
        && run.getLeaseExpiresAt() != null
        && run.getLeaseExpiresAt().isAfter(now);
  }
}
