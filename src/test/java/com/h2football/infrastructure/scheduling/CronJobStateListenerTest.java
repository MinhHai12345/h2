package com.h2football.infrastructure.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.operations.domain.model.CronJob;
import com.h2football.module.operations.domain.model.CronJobExecutionStatus;
import com.h2football.module.operations.domain.model.CronJobStatus;
import com.h2football.module.operations.domain.repository.CronJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

@ExtendWith(MockitoExtension.class)
class CronJobStateListenerTest {
  @Mock private CronJobRepository repository;
  @Mock private JobExecutionContext context;
  @Mock private JobDetail jobDetail;

  @Test
  void recordsSuccessfulExecutionAndRestoresScheduledState() {
    CronJob job = createJob();
    JobKey key = JobKey.jobKey("season-17-52186", "sofascore-ingestion");
    when(context.getJobDetail()).thenReturn(jobDetail);
    when(jobDetail.getKey()).thenReturn(key);
    when(repository.findByJobNameAndJobGroup(key.getName(), key.getGroup()))
        .thenReturn(Optional.of(job));

    Clock clock = Clock.fixed(Instant.parse("2026-11-19T00:00:00Z"), ZoneOffset.UTC);
    CronJobStateListener listener = new CronJobStateListener(repository, clock);

    listener.jobToBeExecuted(context);
    assertEquals(CronJobStatus.RUNNING, job.getStatus());

    listener.jobWasExecuted(context, null);

    assertEquals(CronJobStatus.SCHEDULED, job.getStatus());
    assertEquals(CronJobExecutionStatus.SUCCESS, job.getLastExecutionStatus());
    assertEquals(Instant.parse("2026-11-19T00:00:00Z"), job.getLastExecutedAt());
    verify(repository, org.mockito.Mockito.times(2)).save(job);
  }

  @Test
  void preservesOperatorPauseAfterCurrentExecutionFinishes() {
    CronJob job = createJob();
    job.markPaused();
    JobKey key = JobKey.jobKey("season-17-52186", "sofascore-ingestion");
    when(context.getJobDetail()).thenReturn(jobDetail);
    when(jobDetail.getKey()).thenReturn(key);
    when(repository.findByJobNameAndJobGroup(key.getName(), key.getGroup()))
        .thenReturn(Optional.of(job));

    CronJobStateListener listener =
        new CronJobStateListener(
            repository, Clock.fixed(Instant.parse("2026-11-19T00:00:00Z"), ZoneOffset.UTC));

    listener.jobWasExecuted(context, null);

    assertEquals(CronJobStatus.PAUSED, job.getStatus());
    assertEquals(CronJobExecutionStatus.SUCCESS, job.getLastExecutionStatus());
    verify(repository).save(job);
  }

  private CronJob createJob() {
    return new CronJob(
        "season-17-52186",
        "sofascore-ingestion",
        "com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob",
        "0 15 2 * * ?",
        "Daily season synchronization");
  }
}
