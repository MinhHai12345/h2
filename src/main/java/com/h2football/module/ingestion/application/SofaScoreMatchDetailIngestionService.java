package com.h2football.module.ingestion.application;

import com.h2football.module.ingestion.scheduling.SofaScoreIngestionJobProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs match-detail ingestion in dependency order while isolating retries for each data stream.
 *
 * <p>Statistics, lineups and incidents are independent streams. Player statistics depend on the
 * lineup endpoint because the provider exposes player match statistics in the lineup response.
 */
@Service
public class SofaScoreMatchDetailIngestionService {
  private static final Logger log =
      LoggerFactory.getLogger(SofaScoreMatchDetailIngestionService.class);

  private final SofaScoreMatchStatisticsIngestionService statistics;
  private final SofaScoreMatchLineupIngestionService lineups;
  private final SofaScoreMatchIncidentIngestionService incidents;
  private final SofaScorePlayerMatchStatisticsIngestionService playerStatistics;
  private final SofaScoreIngestionJobProperties properties;

  public SofaScoreMatchDetailIngestionService(
      SofaScoreMatchStatisticsIngestionService statistics,
      SofaScoreMatchLineupIngestionService lineups,
      SofaScoreMatchIncidentIngestionService incidents,
      SofaScorePlayerMatchStatisticsIngestionService playerStatistics,
      SofaScoreIngestionJobProperties properties) {
    this.statistics = statistics;
    this.lineups = lineups;
    this.incidents = incidents;
    this.playerStatistics = playerStatistics;
    this.properties = properties;
  }

  public DetailSyncResult sync(Set<Long> eventIds) {
    if (!properties.getMatchDetails().isEnabled() || eventIds == null || eventIds.isEmpty()) {
      return DetailSyncResult.empty();
    }

    int statisticsSucceeded = 0;
    int lineupsSucceeded = 0;
    int incidentsSucceeded = 0;
    int playerStatisticsSucceeded = 0;
    int skippedPlayerStatistics = 0;
    List<String> failures = new ArrayList<>();
    Set<Long> orderedEventIds = new TreeSet<>();
    for (Long eventId : eventIds) {
      if (eventId == null || eventId <= 0) {
        failures.add("Invalid SofaScore event identifier: " + eventId);
      } else {
        orderedEventIds.add(eventId);
      }
    }

    for (Long eventId : orderedEventIds) {
      ensureNotInterrupted();

      if (executeWithRetry("statistics", eventId, () -> statistics.sync(eventId), failures)) {
        statisticsSucceeded++;
      }

      boolean lineupSucceeded =
          executeWithRetry("lineups", eventId, () -> lineups.sync(eventId), failures);
      if (lineupSucceeded) {
        lineupsSucceeded++;
      }

      if (executeWithRetry("incidents", eventId, () -> incidents.sync(eventId), failures)) {
        incidentsSucceeded++;
      }

      if (lineupSucceeded) {
        if (executeWithRetry(
            "player-statistics", eventId, () -> playerStatistics.sync(eventId), failures)) {
          playerStatisticsSucceeded++;
        }
      } else {
        skippedPlayerStatistics++;
        log.warn(
            "Skipped player statistics for SofaScore event {} because lineup ingestion did not complete",
            eventId);
      }
    }

    DetailSyncResult result =
        new DetailSyncResult(
            orderedEventIds.size(),
            statisticsSucceeded,
            lineupsSucceeded,
            incidentsSucceeded,
            playerStatisticsSucceeded,
            skippedPlayerStatistics,
            List.copyOf(failures));
    log.info(
        "Completed SofaScore match-detail orchestration: matches={}, statistics={}, lineups={}, incidents={}, playerStatistics={}, skippedPlayerStatistics={}, failures={}",
        result.matchesProcessed(),
        result.statisticsSucceeded(),
        result.lineupsSucceeded(),
        result.incidentsSucceeded(),
        result.playerStatisticsSucceeded(),
        result.skippedPlayerStatistics(),
        result.failures().size());
    return result;
  }

  private boolean executeWithRetry(
      String task, long eventId, Supplier<?> operation, List<String> failures) {
    var retry = properties.getMatchDetails();
    int maxAttempts = Math.max(1, Math.min(10, retry.getRetryAttempts()));
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      ensureNotInterrupted();
      try {
        operation.get();
        if (attempt > 1) {
          log.info(
              "SofaScore {} ingestion recovered for event {} on attempt {}/{}",
              task,
              eventId,
              attempt,
              maxAttempts);
        }
        return true;
      } catch (RuntimeException exception) {
        if (attempt == maxAttempts) {
          String message =
              "SofaScore "
                  + task
                  + " ingestion failed for event "
                  + eventId
                  + " after "
                  + maxAttempts
                  + " attempts: "
                  + exception.getMessage();
          failures.add(message);
          log.error(message, exception);
          return false;
        }
        long baseDelay = Math.max(0, Math.min(60_000L, retry.getRetryDelayMillis()));
        long delay = baseDelay * attempt;
        log.warn(
            "SofaScore {} ingestion failed for event {} on attempt {}/{}; retrying in {} ms",
            task,
            eventId,
            attempt,
            maxAttempts,
            delay,
            exception);
        pauseBeforeRetry(delay);
      }
    }
    return false;
  }

  private void pauseBeforeRetry(long delayMillis) {
    if (delayMillis == 0) {
      return;
    }
    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("SofaScore match-detail ingestion was interrupted", exception);
    }
  }

  private void ensureNotInterrupted() {
    if (Thread.currentThread().isInterrupted()) {
      throw new IllegalStateException("SofaScore match-detail ingestion was interrupted");
    }
  }

  public record DetailSyncResult(
      int matchesProcessed,
      int statisticsSucceeded,
      int lineupsSucceeded,
      int incidentsSucceeded,
      int playerStatisticsSucceeded,
      int skippedPlayerStatistics,
      List<String> failures) {
    public static DetailSyncResult empty() {
      return new DetailSyncResult(0, 0, 0, 0, 0, 0, List.of());
    }

    public boolean hasFailures() {
      return !failures.isEmpty();
    }
  }
}
