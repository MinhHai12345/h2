package com.h2football.infrastructure.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.ingestion.scheduling.SofaScoreIngestionJobProperties;
import com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;

@ExtendWith(MockitoExtension.class)
class SofaScoreQuartzJobInitializerTest {
  @Mock private Scheduler scheduler;

  @Test
  void registersConfiguredSeasonAsUtcCronJob() throws Exception {
    SofaScoreIngestionJobProperties properties = new SofaScoreIngestionJobProperties();
    properties.setEnabled(true);
    properties.setCron("0 15 2 * * ?");
    SofaScoreIngestionJobProperties.Season season =
        new SofaScoreIngestionJobProperties.Season();
    season.setTournamentId(17);
    season.setSofaSeasonId(52186);
    season.setCanonicalSeasonId(42);
    properties.setSeasons(java.util.List.of(season));

    when(scheduler.checkExists(any(org.quartz.JobKey.class))).thenReturn(false);
    SofaScoreQuartzJobInitializer initializer =
        new SofaScoreQuartzJobInitializer(scheduler, properties);

    initializer.run(null);

    ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());

    JobDetail job = jobCaptor.getValue();
    assertEquals(SofaScoreSeasonMatchSyncJob.class, job.getJobClass());
    assertEquals(17L, job.getJobDataMap().getLongValue("tournamentId"));
    assertEquals(52186L, job.getJobDataMap().getLongValue("sofaSeasonId"));
    assertEquals(42L, job.getJobDataMap().getLongValue("canonicalSeasonId"));
    assertTrue(triggerCaptor.getValue().getKey().getGroup().equals("sofascore-ingestion"));
  }

  @Test
  void doesNotRegisterJobsWhenDisabled() throws Exception {
    SofaScoreIngestionJobProperties properties = new SofaScoreIngestionJobProperties();
    properties.setEnabled(false);

    new SofaScoreQuartzJobInitializer(scheduler, properties).run(null);

    verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
  }
}
