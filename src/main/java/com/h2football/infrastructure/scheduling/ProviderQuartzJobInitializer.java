package com.h2football.infrastructure.scheduling;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.operations.application.CronJobApplicationService;
import com.h2football.module.operations.domain.model.CronJob;
import com.h2football.module.operations.domain.model.CronJobStatus;
import com.h2football.module.provider.ProviderIngestionProperties;
import com.h2football.module.provider.ProviderIngestionProperties.ProviderConfig;
import com.h2football.module.provider.ProviderIngestionProperties.SeasonJob;
import com.h2football.module.workflow.scheduling.ProviderSeasonWorkflowDispatchJob;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProviderQuartzJobInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(ProviderQuartzJobInitializer.class);
  private final ProviderIngestionProperties properties;
  private final CronJobApplicationService cronJobs;

  public ProviderQuartzJobInitializer(
      ProviderIngestionProperties properties, CronJobApplicationService cronJobs) {
    this.properties = properties;
    this.cronJobs = cronJobs;
  }

  @Override
  public void run(ApplicationArguments args) {
    reconcile(Provider.UNDERSTAT, properties.getUnderstat(), "understat-ingestion");
    reconcile(Provider.FOOTBALL_DATA, properties.getFootballData(), "football-data-ingestion");
    reconcile(Provider.FOOTBALL_DATA_UK, properties.getFootballDataUk(), "football-data-uk-ingestion");
  }

  private void reconcile(Provider provider, ProviderConfig config, String group) {
    if (!config.isEnabled() || config.getSeasons().isEmpty()) {
      cronJobs.pauseGroup(group);
      log.info(
          "Provider ingestion jobs paused: provider={}, enabled={}, configuredSeasons={}",
          provider,
          config.isEnabled(),
          config.getSeasons().size());
      return;
    }

    Set<String> activeNames = new LinkedHashSet<>();
    for (SeasonJob season : config.getSeasons()) {
      validate(provider, season);
      String name = "season-" + season.getCanonicalSeasonId();
      activeNames.add(name);
      JobDataMap data = new JobDataMap();
      data.put("provider", provider.name());
      data.put("canonicalSeasonId", season.getCanonicalSeasonId());
      data.put("externalLeagueId", season.getExternalLeagueId().trim());
      data.put("seasonYear", season.getSeasonYear());
      CronJob job =
          cronJobs.registerOrUpdate(
              name,
              group,
              ProviderSeasonWorkflowDispatchJob.class.getName(),
              config.getCron(),
              "Run provider season ingestion and shared analytics workflow",
              data);
      if (job.getStatus() == CronJobStatus.PAUSED) {
        job = cronJobs.resume(job.getId());
      }
      if (config.isRunOnStartup()) {
        cronJobs.triggerNow(job.getId(), Map.of());
      }
    }
    cronJobs.deleteGroupExcept(group, activeNames);
  }

  private void validate(Provider provider, SeasonJob season) {
    if (season == null
        || season.getCanonicalSeasonId() <= 0
        || season.getSeasonYear() < 1900
        || !StringUtils.hasText(season.getExternalLeagueId())) {
      throw new IllegalArgumentException(
          "Provider season configuration is incomplete for " + provider);
    }
    if (provider == Provider.UNDERSTAT && season.getSeasonYear() < 2014) {
      throw new IllegalArgumentException("Understat seasons must start in 2014 or later");
    }
  }
}
