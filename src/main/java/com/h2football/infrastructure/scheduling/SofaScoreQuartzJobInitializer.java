package com.h2football.infrastructure.scheduling;

import com.h2football.module.ingestion.scheduling.SofaScoreIngestionJobProperties;
import com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Registers configured season sync jobs after the Quartz scheduler is initialized. */
@Component
public class SofaScoreQuartzJobInitializer implements ApplicationRunner {
  private static final Logger log =
      LoggerFactory.getLogger(SofaScoreQuartzJobInitializer.class);
  private static final String JOB_GROUP = "sofascore-ingestion";

  private final Scheduler scheduler;
  private final SofaScoreIngestionJobProperties properties;

  public SofaScoreQuartzJobInitializer(
      Scheduler scheduler, SofaScoreIngestionJobProperties properties) {
    this.scheduler = scheduler;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!properties.isEnabled()) {
      log.info("SofaScore scheduled ingestion is disabled");
      return;
    }
    if (properties.getSeasons().isEmpty()) {
      log.warn("SofaScore scheduled ingestion is enabled but no seasons are configured");
      return;
    }

    CronScheduleBuilder schedule =
        CronScheduleBuilder.cronSchedule(properties.getCron())
            .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
            .withMisfireHandlingInstructionDoNothing();

    for (SofaScoreIngestionJobProperties.Season season : properties.getSeasons()) {
      validate(season);
      String identity =
          "season-"
              + season.getTournamentId()
              + "-"
              + season.getSofaSeasonId()
              + "-"
              + season.getCanonicalSeasonId();
      JobKey jobKey = JobKey.jobKey(identity, JOB_GROUP);
      TriggerKey triggerKey = TriggerKey.triggerKey(identity, JOB_GROUP);

      JobDataMap data = new JobDataMap();
      data.put(SofaScoreSeasonMatchSyncJob.TOURNAMENT_ID, season.getTournamentId());
      data.put(SofaScoreSeasonMatchSyncJob.SOFA_SEASON_ID, season.getSofaSeasonId());
      data.put(SofaScoreSeasonMatchSyncJob.CANONICAL_SEASON_ID, season.getCanonicalSeasonId());

      JobDetail job =
          JobBuilder.newJob(SofaScoreSeasonMatchSyncJob.class)
              .withIdentity(jobKey)
              .usingJobData(data)
              .storeDurably()
              .build();
      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity(triggerKey)
              .forJob(jobKey)
              .startAt(Date.from(java.time.Instant.now()))
              .withSchedule(schedule)
              .build();

      if (scheduler.checkExists(jobKey)) {
        scheduler.addJob(job, true, true);
        if (scheduler.checkExists(triggerKey)) {
          scheduler.rescheduleJob(triggerKey, trigger);
        } else {
          scheduler.scheduleJob(trigger);
        }
      } else {
        scheduler.scheduleJob(job, trigger);
      }
      log.info(
          "Registered SofaScore season job {} with cron {} UTC",
          jobKey,
          properties.getCron());
    }
  }

  private void validate(SofaScoreIngestionJobProperties.Season season) {
    if (season.getTournamentId() <= 0
        || season.getSofaSeasonId() <= 0
        || season.getCanonicalSeasonId() <= 0) {
      throw new IllegalArgumentException(
          "SofaScore job season identifiers must all be positive");
    }
  }
}
