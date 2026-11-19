package com.h2football.module.ingestion.scheduling;

import com.h2football.module.ingestion.application.SofaScoreSeasonMatchIngestionService;
import java.util.concurrent.atomic.AtomicReference;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/** Thin Quartz adapter for the season-level SofaScore ingestion use case. */
@DisallowConcurrentExecution
public class SofaScoreSeasonMatchSyncJob extends QuartzJobBean implements InterruptableJob {
  public static final String TOURNAMENT_ID = "tournamentId";
  public static final String SOFA_SEASON_ID = "sofaSeasonId";
  public static final String CANONICAL_SEASON_ID = "canonicalSeasonId";

  private static final Logger log = LoggerFactory.getLogger(SofaScoreSeasonMatchSyncJob.class);

  private final SofaScoreSeasonMatchIngestionService ingestionService;
  private final AtomicReference<Thread> executionThread = new AtomicReference<>();

  public SofaScoreSeasonMatchSyncJob(SofaScoreSeasonMatchIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    var parameters = context.getMergedJobDataMap();
    long tournamentId = parameters.getLongValue(TOURNAMENT_ID);
    long sofaSeasonId = parameters.getLongValue(SOFA_SEASON_ID);
    long canonicalSeasonId = parameters.getLongValue(CANONICAL_SEASON_ID);
    executionThread.set(Thread.currentThread());

    log.info(
        "Starting SofaScore season synchronization: tournamentId={}, sofaSeasonId={}, canonicalSeasonId={}",
        tournamentId,
        sofaSeasonId,
        canonicalSeasonId);
    try {
      var result = ingestionService.sync(tournamentId, sofaSeasonId, canonicalSeasonId);
      log.info(
          "Completed SofaScore season synchronization: tournamentId={}, sofaSeasonId={}, discovered={}, created={}, updated={}, skipped={}, warnings={}",
          tournamentId,
          sofaSeasonId,
          result.discoveredEvents(),
          result.createdMatches(),
          result.updatedMatches(),
          result.skippedEvents(),
          result.warnings().size());
    } catch (RuntimeException exception) {
      log.error(
          "SofaScore season synchronization failed: tournamentId={}, sofaSeasonId={}",
          tournamentId,
          sofaSeasonId,
          exception);
      throw new JobExecutionException("SofaScore season synchronization failed", exception, false);
    } finally {
      executionThread.set(null);
    }
  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {
    Thread runningThread = executionThread.get();
    if (runningThread == null) {
      throw new UnableToInterruptJobException("SofaScore synchronization is not running");
    }
    runningThread.interrupt();
  }
}
