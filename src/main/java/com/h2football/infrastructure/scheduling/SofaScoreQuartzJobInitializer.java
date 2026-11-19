package com.h2football.infrastructure.scheduling;

import com.h2football.module.ingestion.scheduling.SofaScoreIngestionJobProperties;
import com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob;
import com.h2football.module.operations.application.CronJobApplicationService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Reconciles configured SofaScore jobs with the application job registry. */
@Component
public class SofaScoreQuartzJobInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(SofaScoreQuartzJobInitializer.class);
  private static final String JOB_GROUP = "sofascore-ingestion";

  private final SofaScoreIngestionJobProperties properties;
  private final CronJobApplicationService cronJobs;

  public SofaScoreQuartzJobInitializer(
      SofaScoreIngestionJobProperties properties, CronJobApplicationService cronJobs) {
    this.properties = properties;
    this.cronJobs = cronJobs;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isEnabled()) {
      cronJobs.pauseGroup(JOB_GROUP);
      log.info("SofaScore scheduled ingestion is disabled; existing jobs were paused");
      return;
    }
    if (properties.getSeasons().isEmpty()) {
      cronJobs.pauseGroup(JOB_GROUP);
      log.warn(
          "SofaScore scheduled ingestion has no configured seasons; existing jobs were paused");
      return;
    }

    Set<String> activeJobNames = new LinkedHashSet<>();
    for (SofaScoreIngestionJobProperties.Season season : properties.getSeasons()) {
      validate(season);
      String name = "season-" + season.getTournamentId() + "-" + season.getSofaSeasonId();
      activeJobNames.add(name);
      JobDataMap data = new JobDataMap();
      data.put(SofaScoreSeasonMatchSyncJob.TOURNAMENT_ID, season.getTournamentId());
      data.put(SofaScoreSeasonMatchSyncJob.SOFA_SEASON_ID, season.getSofaSeasonId());
      data.put(SofaScoreSeasonMatchSyncJob.CANONICAL_SEASON_ID, season.getCanonicalSeasonId());
      cronJobs.registerOrUpdate(
          name,
          JOB_GROUP,
          SofaScoreSeasonMatchSyncJob.class.getName(),
          properties.getCron(),
          "Synchronize SofaScore teams and match snapshots for configured season",
          data);
      log.info("Reconciled SofaScore cron job {} with cron {} UTC", name, properties.getCron());
    }
    cronJobs.deleteGroupExcept(JOB_GROUP, activeJobNames);
  }

  private void validate(SofaScoreIngestionJobProperties.Season season) {
    if (season.getTournamentId() <= 0
        || season.getSofaSeasonId() <= 0
        || season.getCanonicalSeasonId() <= 0) {
      throw new IllegalArgumentException("SofaScore job season identifiers must all be positive");
    }
  }
}
