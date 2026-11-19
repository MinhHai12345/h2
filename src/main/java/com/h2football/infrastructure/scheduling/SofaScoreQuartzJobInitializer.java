package com.h2football.infrastructure.scheduling;

import com.h2football.module.ingestion.scheduling.SofaScoreIngestionJobProperties;
import com.h2football.module.ingestion.scheduling.SofaScoreLeagueCatalogueSyncJob;
import com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob;
import com.h2football.module.operations.application.CronJobApplicationService;
import com.h2football.module.workflow.scheduling.SofaScoreSeasonWorkflowDispatchJob;
import com.h2football.module.operations.domain.model.CronJob;
import com.h2football.module.operations.domain.model.CronJobStatus;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Reconciles all configured SofaScore ingestion jobs with the application job registry. */
@Component
public class SofaScoreQuartzJobInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(SofaScoreQuartzJobInitializer.class);
  private static final String LEAGUE_GROUP = "sofascore-masterdata";
  private static final String SEASON_GROUP = "sofascore-ingestion";

  private final SofaScoreIngestionJobProperties properties;
  private final CronJobApplicationService cronJobs;

  public SofaScoreQuartzJobInitializer(
      SofaScoreIngestionJobProperties properties, CronJobApplicationService cronJobs) {
    this.properties = properties;
    this.cronJobs = cronJobs;
  }

  @Override
  public void run(ApplicationArguments args) {
    reconcileLeagueCatalogueJob();
    reconcileSeasonJobs();
  }

  private void reconcileLeagueCatalogueJob() {
    SofaScoreIngestionJobProperties.LeagueCatalogue catalogue = properties.getLeagueCatalogue();
    if (!catalogue.isEnabled()) {
      cronJobs.pauseGroup(LEAGUE_GROUP);
      log.info("SofaScore league catalogue job is disabled; existing job was paused");
      return;
    }

    validateTournamentIds(catalogue.getTournamentIds());
    JobDataMap data = new JobDataMap();
    data.put(
        SofaScoreLeagueCatalogueSyncJob.TOURNAMENT_IDS,
        catalogue.getTournamentIds().stream()
            .map(String::valueOf)
            .collect(java.util.stream.Collectors.joining(",")));
    var job =
        cronJobs.registerOrUpdate(
            "league-catalogue",
            LEAGUE_GROUP,
            SofaScoreLeagueCatalogueSyncJob.class.getName(),
            catalogue.getCron(),
            "Synchronize SofaScore tournament and season catalogue",
            data);
    job = resumeIfPaused(job);

    if (catalogue.isRunOnStartup()) {
      cronJobs.triggerNow(job.getId(), Map.of());
      log.info("Triggered initial SofaScore league catalogue synchronization");
    }
  }

  private void reconcileSeasonJobs() {
    if (!properties.isEnabled()) {
      cronJobs.pauseGroup(SEASON_GROUP);
      log.info("SofaScore season ingestion is disabled; existing jobs were paused");
      return;
    }
    if (properties.getSeasons().isEmpty()) {
      cronJobs.pauseGroup(SEASON_GROUP);
      log.warn("SofaScore season ingestion has no configured seasons; existing jobs were paused");
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
      var job =
          cronJobs.registerOrUpdate(
              name,
              SEASON_GROUP,
              SofaScoreSeasonWorkflowDispatchJob.class.getName(),
              properties.getCron(),
              "Run the complete SofaScore season ingestion and analytics workflow",
              data);
      job = resumeIfPaused(job);
      if (properties.isRunOnStartup()) {
        cronJobs.triggerNow(job.getId(), Map.of());
        log.info("Triggered initial SofaScore season synchronization for {}", name);
      }
      log.info("Reconciled SofaScore cron job {} with cron {} UTC", name, properties.getCron());
    }
    cronJobs.deleteGroupExcept(SEASON_GROUP, activeJobNames);
  }

  private CronJob resumeIfPaused(CronJob job) {
    return job.getStatus() == CronJobStatus.PAUSED ? cronJobs.resume(job.getId()) : job;
  }

  private void validate(SofaScoreIngestionJobProperties.Season season) {
    if (season.getTournamentId() <= 0
        || season.getSofaSeasonId() <= 0
        || season.getCanonicalSeasonId() <= 0) {
      throw new IllegalArgumentException("SofaScore job season identifiers must all be positive");
    }
  }

  private void validateTournamentIds(java.util.List<Long> tournamentIds) {
    if (tournamentIds.stream().anyMatch(id -> id == null || id <= 0)) {
      throw new IllegalArgumentException("SofaScore tournament identifiers must be positive");
    }
  }
}
