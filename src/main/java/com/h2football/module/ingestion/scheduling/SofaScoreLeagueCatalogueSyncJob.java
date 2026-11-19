package com.h2football.module.ingestion.scheduling;

import com.h2football.module.ingestion.application.SofaScoreMasterDataIngestionService;
import java.util.Arrays;
import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/** Quartz adapter for importing the SofaScore tournament catalogue. */
@DisallowConcurrentExecution
public class SofaScoreLeagueCatalogueSyncJob extends QuartzJobBean {
  public static final String TOURNAMENT_IDS = "tournamentIds";

  private static final Logger log = LoggerFactory.getLogger(SofaScoreLeagueCatalogueSyncJob.class);

  private final SofaScoreMasterDataIngestionService ingestionService;

  public SofaScoreLeagueCatalogueSyncJob(SofaScoreMasterDataIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    String configuredIds = context.getMergedJobDataMap().getString(TOURNAMENT_IDS);
    List<Long> tournamentIds = parseTournamentIds(configuredIds);
    try {
      var result = ingestionService.syncLeagues(tournamentIds);
      log.info(
          "Completed SofaScore league catalogue synchronization: discovered={}, imported={}, skipped={}, discoveredSeasons={}, warnings={}",
          result.discoveredLeagues(),
          result.importedLeagues(),
          result.skippedLeagues(),
          result.discoveredSeasons(),
          result.warnings().size());
    } catch (RuntimeException exception) {
      log.error("SofaScore league catalogue synchronization failed", exception);
      throw new JobExecutionException(
          "SofaScore league catalogue synchronization failed", exception, false);
    }
  }

  private List<Long> parseTournamentIds(String value) throws JobExecutionException {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    try {
      return Arrays.stream(value.split(","))
          .map(String::trim)
          .filter(token -> !token.isEmpty())
          .map(Long::parseLong)
          .peek(
              id -> {
                if (id <= 0) {
                  throw new IllegalArgumentException("Tournament identifiers must be positive");
                }
              })
          .distinct()
          .toList();
    } catch (RuntimeException exception) {
      throw new JobExecutionException("Invalid SofaScore tournamentIds job parameter", exception);
    }
  }
}
