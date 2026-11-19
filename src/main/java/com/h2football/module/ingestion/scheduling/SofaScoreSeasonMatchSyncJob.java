package com.h2football.module.ingestion.scheduling;

import com.h2football.module.ingestion.application.SofaScoreSeasonMatchIngestionService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/** Quartz adapter for the season-level SofaScore ingestion use case. */
@DisallowConcurrentExecution
public class SofaScoreSeasonMatchSyncJob extends QuartzJobBean {
  public static final String TOURNAMENT_ID = "tournamentId";
  public static final String SOFA_SEASON_ID = "sofaSeasonId";
  public static final String CANONICAL_SEASON_ID = "canonicalSeasonId";

  private static final Logger log = LoggerFactory.getLogger(SofaScoreSeasonMatchSyncJob.class);

  private final SofaScoreSeasonMatchIngestionService ingestionService;

  public SofaScoreSeasonMatchSyncJob(SofaScoreSeasonMatchIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    var parameters = context.getMergedJobDataMap();
    long tournamentId = parameters.getLongValue(TOURNAMENT_ID);
    long sofaSeasonId = parameters.getLongValue(SOFA_SEASON_ID);
    long canonicalSeasonId = parameters.getLongValue(CANONICAL_SEASON_ID);

    log.info(
        "Starting scheduled SofaScore season sync: tournamentId={}, sofaSeasonId={}, canonicalSeasonId={}",
        tournamentId,
        sofaSeasonId,
        canonicalSeasonId);
    try {
      var result = ingestionService.sync(tournamentId, sofaSeasonId, canonicalSeasonId);
      log.info(
          "Completed scheduled SofaScore season sync: tournamentId={}, sofaSeasonId={}, discovered={}, created={}, updated={}, skipped={}, warnings={}",
          tournamentId,
          sofaSeasonId,
          result.discoveredEvents(),
          result.createdMatches(),
          result.updatedMatches(),
          result.skippedEvents(),
          result.warnings().size());
    } catch (RuntimeException exception) {
      log.error(
          "Scheduled SofaScore season sync failed: tournamentId={}, sofaSeasonId={}, canonicalSeasonId={}",
          tournamentId,
          sofaSeasonId,
          canonicalSeasonId,
          exception);
      throw new JobExecutionException("SofaScore season synchronization failed", exception, false);
    }
  }
}
