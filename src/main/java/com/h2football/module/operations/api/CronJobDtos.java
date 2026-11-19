package com.h2football.module.operations.api;

import com.h2football.module.operations.domain.model.CronJob;
import com.h2football.module.operations.domain.model.CronJobExecutionStatus;
import com.h2football.module.operations.domain.model.CronJobStatus;
import java.time.Instant;

public final class CronJobDtos {
  private CronJobDtos() {}

  public record CronJobResponse(
      Long id,
      String jobName,
      String jobGroup,
      String cronExpression,
      String description,
      CronJobStatus status,
      Instant lastExecutedAt,
      CronJobExecutionStatus lastExecutionStatus,
      String lastExecutionMessage) {
    public static CronJobResponse from(CronJob job) {
      return new CronJobResponse(
          job.getId(),
          job.getJobName(),
          job.getJobGroup(),
          job.getCronExpression(),
          job.getDescription(),
          job.getStatus(),
          job.getLastExecutedAt(),
          job.getLastExecutionStatus(),
          job.getLastExecutionMessage());
    }
  }

  public record TriggerRequest(Long tournamentId, Long sofaSeasonId, Long canonicalSeasonId) {}
}
