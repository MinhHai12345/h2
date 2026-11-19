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
    return runs.findByWorkflowTypeAndIdempotencyKey(type, key)
        .orElseThrow(() -> new IllegalStateException("Workflow run could not be created or loaded"));
  }

  @Transactional
  public void acquireLease(Long runId, String owner, Duration duration) {
    WorkflowRun run = lockRun(runId);
    Instant now = Instant.now(clock);
    run.claim(owner, now.plus(duration), now);
  }

  @Transactional(readOnly = true)
  public boolean isStageCompleted(Long runId, String stageKey) {
    return stages.findByWorkflowRun_IdAndStageKey(runId, stageKey)
        .map(stage -> stage.getStatus() == WorkflowStageStatus.SUCCEEDED)
        .orElse(false);
  }

  @Transactional
  public int startStage(Long runId, String stageKey, int order) {
    WorkflowRun run = lockRun(runId);
    WorkflowStageExecution stage =
        stages.findByWorkflowRun_IdAndStageKey(runId, stageKey)
            .orElseGet(() -> new WorkflowStageExecution(run, stageKey, order));
    stage.start(Instant.now(clock));
    run.selectStage(stageKey);
    return stages.save(stage).getAttemptCount();
  }

  @Transactional
  public void completeStage(Long runId, String stageKey, String checkpointPayload) {
    WorkflowStageExecution stage = findStage(runId, stageKey);
    stage.succeed(checkpointPayload, Instant.now(clock));
    stages.save(stage);
  }

  @Transactional
  public void failStage(Long runId, String stageKey, String message) {
    WorkflowStageExecution stage = findStage(runId, stageKey);
    stage.fail(message);
    stages.save(stage);
  }

  @Transactional
  public void completeRun(Long runId) {
    WorkflowRun run = lockRun(runId);
    run.complete(Instant.now(clock));
  }

  @Transactional
  public void failRun(Long runId, String message) {
    lockRun(runId).fail(message);
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
    return stages.findByWorkflowRun_IdAndStageKey(runId, stageKey)
        .orElseThrow(() -> new IllegalArgumentException("Workflow stage not found: " + stageKey));
  }
}
