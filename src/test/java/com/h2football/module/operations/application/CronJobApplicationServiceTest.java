package com.h2football.module.operations.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.operations.domain.model.CronJob;
import com.h2football.module.operations.domain.model.CronJobStatus;
import com.h2football.module.operations.domain.repository.CronJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;

@ExtendWith(MockitoExtension.class)
class CronJobApplicationServiceTest {
  @Mock private Scheduler scheduler;
  @Mock private CronJobRepository repository;

  @Test
  void registersJobAndCronTriggerInQuartz() throws Exception {
    when(repository.findByJobNameAndJobGroup("season-17-52186", "sofascore-ingestion"))
        .thenReturn(Optional.empty());
    when(repository.save(any(CronJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(scheduler.checkExists(any(org.quartz.JobKey.class))).thenReturn(false);

    CronJobApplicationService service = new CronJobApplicationService(scheduler, repository, new ObjectMapper());
    JobDataMap data = new JobDataMap();
    data.put("tournamentId", 17L);
    data.put("sofaSeasonId", 52186L);
    data.put("canonicalSeasonId", 42L);

    service.registerOrUpdate(
        "season-17-52186",
        "sofascore-ingestion",
        "com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob",
        "0 15 2 * * ?",
        "Daily season synchronization",
        data);

    ArgumentCaptor<JobDetail> detail = ArgumentCaptor.forClass(JobDetail.class);
    ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).scheduleJob(detail.capture(), trigger.capture());
    assertEquals("season-17-52186", detail.getValue().getKey().getName());
    assertEquals(52186L, detail.getValue().getJobDataMap().getLongValue("sofaSeasonId"));
    assertEquals("0 15 2 * * ?", ((org.quartz.CronTrigger) trigger.getValue()).getCronExpression());
    assertEquals("UTC", ((org.quartz.CronTrigger) trigger.getValue()).getTimeZone().getID());
  }

  @Test
  void rejectsUnknownJobClassBeforeRegistering() {
    CronJobApplicationService service = new CronJobApplicationService(scheduler, repository, new ObjectMapper());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.registerOrUpdate(
                "unexpected",
                "operations",
                "java.lang.Runtime",
                "0 0 1 * * ?",
                "Invalid class",
                new JobDataMap()));

    verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
  }

  @Test
  void rejectsInvalidCronExpression() {
    CronJobApplicationService service = new CronJobApplicationService(scheduler, repository, new ObjectMapper());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.registerOrUpdate(
                "invalid-cron",
                "operations",
                "com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob",
                "not-a-cron",
                "Invalid schedule",
                new JobDataMap()));

    verify(repository, never()).save(any(CronJob.class));
  }

  @Test
  void doesNotTriggerPausedJob() throws Exception {
    CronJob job =
        new CronJob(
            "season-17-52186",
            "sofascore-ingestion",
            "com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob",
            "0 15 2 * * ?",
            "Daily season synchronization");
    job.markPaused();
    when(repository.findById(7L)).thenReturn(Optional.of(job));

    CronJobApplicationService service = new CronJobApplicationService(scheduler, repository, new ObjectMapper());

    assertThrows(CronJobOperationException.class, () -> service.triggerNow(7L, Map.of()));
    verify(scheduler, never()).triggerJob(any(org.quartz.JobKey.class), any(JobDataMap.class));
    assertEquals(CronJobStatus.PAUSED, job.getStatus());
  }
}
