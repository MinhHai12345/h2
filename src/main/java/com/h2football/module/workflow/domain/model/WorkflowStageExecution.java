package com.h2football.module.workflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(
    name = "workflow_stage_executions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_workflow_stage_execution",
            columnNames = {"workflow_run_id", "stage_key"}))
public class WorkflowStageExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_run_id", nullable = false)
  private WorkflowRun workflowRun;

  @Column(name = "stage_key", nullable = false, length = 100)
  private String stageKey;

  @Column(name = "stage_order", nullable = false)
  private int stageOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkflowStageStatus status;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "checkpoint_payload", columnDefinition = "JSON")
  private String checkpointPayload;

  @Column(name = "last_error", length = 2000)
  private String lastError;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected WorkflowStageExecution() {}

  public WorkflowStageExecution(WorkflowRun workflowRun, String stageKey, int stageOrder) {
    if (workflowRun == null || stageKey == null || stageKey.isBlank() || stageOrder < 0) {
      throw new IllegalArgumentException("Workflow run, stage key and non-negative order are required");
    }
    this.workflowRun = workflowRun;
    this.stageKey = stageKey;
    this.stageOrder = stageOrder;
    this.status = WorkflowStageStatus.PENDING;
  }

  public void start(Instant now) {
    status = WorkflowStageStatus.RUNNING;
    attemptCount++;
    startedAt = now;
    completedAt = null;
    lastError = null;
  }

  public void succeed(String payload, Instant now) {
    status = WorkflowStageStatus.SUCCEEDED;
    checkpointPayload = payload;
    completedAt = now;
    lastError = null;
  }

  public void fail(String message) {
    status = WorkflowStageStatus.FAILED;
    lastError = message == null ? "Stage execution failed" : message.substring(0, Math.min(2000, message.length()));
  }

  public Long getId() { return id; }
  public WorkflowRun getWorkflowRun() { return workflowRun; }
  public String getStageKey() { return stageKey; }
  public int getStageOrder() { return stageOrder; }
  public WorkflowStageStatus getStatus() { return status; }
  public int getAttemptCount() { return attemptCount; }
  public String getCheckpointPayload() { return checkpointPayload; }
  public String getLastError() { return lastError; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public long getVersion() { return version; }
}
