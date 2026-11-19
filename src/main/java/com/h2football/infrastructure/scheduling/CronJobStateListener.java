package com.h2football.infrastructure.scheduling;

import com.h2football.module.operations.domain.model.CronJobExecutionStatus;
import com.h2football.module.operations.domain.repository.CronJobRepository;
import java.time.Clock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CronJobStateListener extends JobListenerSupport {
  private final CronJobRepository repository;
  private final Clock clock;

  public CronJobStateListener(CronJobRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public String getName() {
    return "h2-cron-job-state-listener";
  }

  @Override
  @Transactional
  public void jobToBeExecuted(JobExecutionContext context) {
    find(context.getJobDetail().getKey())
        .ifPresent(
            job -> {
              job.markRunning();
              repository.save(job);
            });
  }

  @Override
  @Transactional
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception) {
    find(context.getJobDetail().getKey())
        .ifPresent(
            job -> {
              Object result = context.getResult();
              boolean failed = exception != null || result == CronJobExecutionStatus.ERROR;
              String message = exception == null ? null : exception.getMessage();
              job.recordExecution(
                  failed ? CronJobExecutionStatus.ERROR : CronJobExecutionStatus.SUCCESS,
                  message,
                  clock.instant());
              repository.save(job);
            });
  }

  private java.util.Optional<com.h2football.module.operations.domain.model.CronJob> find(
      JobKey key) {
    return repository.findByJobNameAndJobGroup(key.getName(), key.getGroup());
  }
}
