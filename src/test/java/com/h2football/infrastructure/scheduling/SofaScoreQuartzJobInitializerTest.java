package com.h2football.infrastructure.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.ingestion.scheduling.SofaScoreIngestionJobProperties;
import com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob;
import com.h2football.module.operations.application.CronJobApplicationService;
import com.h2football.module.operations.domain.model.CronJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;

@ExtendWith(MockitoExtension.class)
class SofaScoreQuartzJobInitializerTest {
  @Mock private CronJobApplicationService cronJobs;

  @Test
  void registersConfiguredSeasonInPersistentCronJobRegistry() {
    SofaScoreIngestionJobProperties properties = new SofaScoreIngestionJobProperties();
    properties.setEnabled(true);
    properties.setCron("0 15 2 * * ?");
    SofaScoreIngestionJobProperties.Season season = new SofaScoreIngestionJobProperties.Season();
    season.setTournamentId(17);
    season.setSofaSeasonId(52186);
    season.setCanonicalSeasonId(42);
    properties.setSeasons(java.util.List.of(season));

    when(
            cronJobs.registerOrUpdate(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(JobDataMap.class)))
        .thenReturn(
            new CronJob(
                "season-17-52186",
                "sofascore-ingestion",
                SofaScoreSeasonMatchSyncJob.class.getName(),
                "0 15 2 * * ?",
                "Synchronize SofaScore teams and match snapshots for configured season"));

    new SofaScoreQuartzJobInitializer(properties, cronJobs).run(null);

    org.mockito.ArgumentCaptor<JobDataMap> data =
        org.mockito.ArgumentCaptor.forClass(JobDataMap.class);
    verify(cronJobs)
        .registerOrUpdate(
            eq("season-17-52186"),
            eq("sofascore-ingestion"),
            eq(SofaScoreSeasonMatchSyncJob.class.getName()),
            eq("0 15 2 * * ?"),
            eq("Synchronize SofaScore teams and match snapshots for configured season"),
            data.capture());
    org.junit.jupiter.api.Assertions.assertEquals(
        42L, data.getValue().getLongValue("canonicalSeasonId"));
  }

  @Test
  void doesNotRegisterJobsWhenDisabled() {
    SofaScoreIngestionJobProperties properties = new SofaScoreIngestionJobProperties();
    properties.setEnabled(false);

    new SofaScoreQuartzJobInitializer(properties, cronJobs).run(null);

    verify(cronJobs, never())
        .registerOrUpdate(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
  }
}
