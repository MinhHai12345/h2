package com.h2football.module.workflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "workflow_runs",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_workflow_run_idempotency",
            columnNames = {"workflow_type", "idempotency_key"}),
    indexes = {
      @Index(name = "idx_workflow_runs_status_lease", columnList = "status, lease_expires_at"),
      @Index(name = "idx_workflow_runs_created_at", columnList = "created_at")
    })
public class WorkflowRun {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "workflow_type", nullable = false, length = 80)
  private String workflowType;

  @Column(name = "idempotency_key", nullable = false, length = 160)
  private String idempotencyKey;

  @Column(name = "request_payload", nullable = false, columnDefinition = "JSON")
  private String requestPayload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkflowRunStatus status;

  @Column(name = "current_stage", length = 100)
  private String currentStage;

  @Column(name = "lease_owner", length = 64)
  private String leaseOwner;

  @Column(name = "lease_expires_at")
  private Instant leaseExpiresAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "last_error", length = 2000)
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected WorkflowRun() {}

  public WorkflowRun(
      String workflowType, String idempotencyKey, String requestPayload, Instant createdAt) {
    if (workflowType == null || workflowType.isBlank()
        || idempotencyKey == null || idempotencyKey.isBlank()
        || requestPayload == null || createdAt == null) {
      throw new IllegalArgumentException("Workflow type, idempotency key, payload and createdAt are required");
    }
    if (workflowType.length() > 80 || idempotencyKey.length() > 160) {
      throw new IllegalArgumentException("Workflow type or idempotency key exceeds its maximum length");
    }
    this.workflowType = workflowType;
    this.idempotencyKey = idempotencyKey;
    this.requestPayload = requestPayload;
    this.createdAt = createdAt;
    this.status = WorkflowRunStatus.PENDING;
  }

  public void claim(String owner, Instant expiresAt, Instant now) {
    if (owner == null || owner.isBlank() || expiresAt == null || now == null) {
      throw new IllegalArgumentException("Lease owner and timestamps are required");
    }
    if (status == WorkflowRunStatus.SUCCEEDED) {
      throw new IllegalStateException("A completed workflow cannot be claimed");
    }
    if (leaseOwner != null && leaseExpiresAt != null
        && leaseExpiresAt.isAfter(now) && !leaseOwner.equals(owner)) {
      throw new IllegalStateException("Workflow is already leased by another worker");
    }
    status = WorkflowRunStatus.RUNNING;
    leaseOwner = owner;
    leaseExpiresAt = expiresAt;
    if (startedAt == null) {
      startedAt = now;
    }
    lastError = null;
  }

  public void selectStage(String stageKey) {
    currentStage = stageKey;
  }

  public void complete(Instant now) {
    status = WorkflowRunStatus.SUCCEEDED;
    completedAt = now;
    leaseOwner = null;
    leaseExpiresAt = null;
    lastError = null;
  }

  public void fail(String message) {
    status = WorkflowRunStatus.FAILED;
    lastError = message == null ? "Workflow execution failed" : message.substring(0, Math.min(2000, message.length()));
    leaseOwner = null;
    leaseExpiresAt = null;
  }

  public Long getId() { return id; }
  public String getWorkflowType() { return workflowType; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public String getRequestPayload() { return requestPayload; }
  public WorkflowRunStatus getStatus() { return status; }
  public String getCurrentStage() { return currentStage; }
  public String getLeaseOwner() { return leaseOwner; }
  public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public String getLastError() { return lastError; }
  public Instant getCreatedAt() { return createdAt; }
  public long getVersion() { return version; }
}
