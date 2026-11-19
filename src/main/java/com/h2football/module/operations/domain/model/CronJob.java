package com.h2football.module.operations.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(
    name = "cron_jobs",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_cron_jobs_name_group", columnNames = {"job_name", "job_group"}))
public class CronJob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_name", nullable = false, length = 120)
  private String jobName;

  @Column(name = "job_group", nullable = false, length = 120)
  private String jobGroup;

  @Column(name = "job_class", nullable = false, length = 255)
  private String jobClass;

  @Column(name = "cron_expression", nullable = false, length = 120)
  private String cronExpression;

  @Column(length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CronJobStatus status;

  @Column(name = "last_executed_at")
  private Instant lastExecutedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_execution_status", length = 20)
  private CronJobExecutionStatus lastExecutionStatus;

  @Column(name = "last_execution_message", length = 1000)
  private String lastExecutionMessage;

  @Version
  @Column(nullable = false)
  private long version;

  protected CronJob() {}

  public CronJob(
      String jobName,
      String jobGroup,
      String jobClass,
      String cronExpression,
      String description) {
    this.jobName = jobName;
    this.jobGroup = jobGroup;
    this.jobClass = jobClass;
    this.cronExpression = cronExpression;
    this.description = description;
    this.status = CronJobStatus.SCHEDULED;
  }

  public Long getId() { return id; }
  public String getJobName() { return jobName; }
  public String getJobGroup() { return jobGroup; }
  public String getJobClass() { return jobClass; }
  public String getCronExpression() { return cronExpression; }
  public String getDescription() { return description; }
  public CronJobStatus getStatus() { return status; }
  public Instant getLastExecutedAt() { return lastExecutedAt; }
  public CronJobExecutionStatus getLastExecutionStatus() { return lastExecutionStatus; }
  public String getLastExecutionMessage() { return lastExecutionMessage; }
  public long getVersion() { return version; }

  public void updateDefinition(String jobClass, String cronExpression, String description) {
    this.jobClass = jobClass;
    this.cronExpression = cronExpression;
    this.description = description;
  }

  public void markRunning() { this.status = CronJobStatus.RUNNING; }

  public void markScheduled() { this.status = CronJobStatus.SCHEDULED; }

  public void markPaused() { this.status = CronJobStatus.PAUSED; }

  public void recordExecution(CronJobExecutionStatus executionStatus, String message, Instant executedAt) {
    this.status = CronJobStatus.SCHEDULED;
    this.lastExecutionStatus = executionStatus;
    this.lastExecutionMessage = message;
    this.lastExecutedAt = executedAt;
  }
}
