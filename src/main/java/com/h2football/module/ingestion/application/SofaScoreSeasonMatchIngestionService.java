package com.h2football.module.ingestion.application;

import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Coordinates the prerequisite team import and complete event-page traversal for one SofaScore
 * tournament season.
 */
@Service
public class SofaScoreSeasonMatchIngestionService {
  private static final int MAX_PAGES_PER_WINDOW = 500;

  private final SofaScoreMasterDataIngestionService masterData;
  private final SofaScoreMatchIngestionService matches;

  public SofaScoreSeasonMatchIngestionService(
      SofaScoreMasterDataIngestionService masterData, SofaScoreMatchIngestionService matches) {
    this.masterData = masterData;
    this.matches = matches;
  }

  public SeasonMatchSyncResult sync(long tournamentId, long sofaSeasonId, long canonicalSeasonId) {
    if (tournamentId <= 0 || sofaSeasonId <= 0 || canonicalSeasonId <= 0) {
      throw new IllegalArgumentException("All season identifiers must be positive");
    }

    var teams = masterData.syncTeams(tournamentId, sofaSeasonId);
    ensureNotInterrupted();
    List<WindowSyncResult> windows = new ArrayList<>();
    Set<Long> eventIds = new LinkedHashSet<>();

    for (SofaScoreEventWindow window : SofaScoreEventWindow.values()) {
      int discovered = 0;
      int created = 0;
      int updated = 0;
      int skipped = 0;
      List<String> warnings = new ArrayList<>();
      boolean exhausted = false;

      for (int page = 0; page < MAX_PAGES_PER_WINDOW; page++) {
        ensureNotInterrupted();
        var result = matches.sync(tournamentId, sofaSeasonId, canonicalSeasonId, window, page);
        if (result.discoveredEvents() == 0) {
          exhausted = true;
          break;
        }
        discovered += result.discoveredEvents();
        created += result.createdMatches();
        updated += result.updatedMatches();
        skipped += result.skippedEvents();
        warnings.addAll(result.warnings());
        eventIds.addAll(result.eventIds());
      }

      if (!exhausted) {
        warnings.add(
            "Stopped "
                + window.pathSegment()
                + " event traversal after "
                + MAX_PAGES_PER_WINDOW
                + " pages to protect the ingestion process.");
      }
      windows.add(
          new WindowSyncResult(
              window, discovered, created, updated, skipped, !exhausted, List.copyOf(warnings)));
    }

    int discovered = windows.stream().mapToInt(WindowSyncResult::discoveredEvents).sum();
    int created = windows.stream().mapToInt(WindowSyncResult::createdMatches).sum();
    int updated = windows.stream().mapToInt(WindowSyncResult::updatedMatches).sum();
    int skipped = windows.stream().mapToInt(WindowSyncResult::skippedEvents).sum();
    List<String> warnings = new ArrayList<>(teams.warnings());
    windows.forEach(result -> warnings.addAll(result.warnings()));

    return new SeasonMatchSyncResult(
        tournamentId,
        sofaSeasonId,
        canonicalSeasonId,
        teams,
        discovered,
        created,
        updated,
        skipped,
        eventIds.size(),
        List.copyOf(windows),
        List.copyOf(warnings));
  }

  private void ensureNotInterrupted() {
    if (Thread.currentThread().isInterrupted()) {
      throw new IllegalStateException("SofaScore season synchronization was interrupted");
    }
  }

  public record WindowSyncResult(
      SofaScoreEventWindow window,
      int discoveredEvents,
      int createdMatches,
      int updatedMatches,
      int skippedEvents,
      boolean pageLimitReached,
      List<String> warnings) {}

  public record SeasonMatchSyncResult(
      long tournamentId,
      long sofaSeasonId,
      long canonicalSeasonId,
      SofaScoreMasterDataIngestionService.TeamSyncResult teams,
      int discoveredEvents,
      int createdMatches,
      int updatedMatches,
      int skippedEvents,
      int uniqueEvents,
      List<WindowSyncResult> windows,
      List<String> warnings) {}
}
