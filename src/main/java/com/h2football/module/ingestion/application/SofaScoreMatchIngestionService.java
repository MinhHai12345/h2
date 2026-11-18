package com.h2football.module.ingestion.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.application.SeasonApplicationService;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.match.application.MatchApplicationService;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.provider.sofascore.SofaScoreMatchProvider;
import com.h2football.module.provider.sofascore.model.SofaScoreEventSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SofaScoreMatchIngestionService {
  private static final Provider PROVIDER = Provider.SOFASCORE;
  private final SofaScoreMatchProvider provider;
  private final SeasonApplicationService seasons;
  private final ExternalIdentityService identities;
  private final MatchApplicationService matches;

  public SofaScoreMatchIngestionService(
      SofaScoreMatchProvider provider,
      SeasonApplicationService seasons,
      ExternalIdentityService identities,
      MatchApplicationService matches) {
    this.provider = provider;
    this.seasons = seasons;
    this.identities = identities;
    this.matches = matches;
  }

  public MatchSyncResult sync(
      long tournamentId,
      long sofaScoreSeasonId,
      long canonicalSeasonId,
      SofaScoreEventWindow window,
      int page) {
    if (tournamentId <= 0 || sofaScoreSeasonId <= 0 || canonicalSeasonId <= 0) {
      throw new IllegalArgumentException(
          "Tournament, SofaScore season and canonical season IDs must be positive");
    }
    if (window == null) throw new IllegalArgumentException("window must not be null");
    if (page < 0) throw new IllegalArgumentException("page must not be negative");

    Season canonicalSeason = seasons.get(canonicalSeasonId);
    Long canonicalLeagueId =
        identities
            .findCanonicalId(ResourceType.LEAGUE, PROVIDER, Long.toString(tournamentId))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "SofaScore tournament is not mapped; sync leagues before matches"));
    if (!canonicalSeason.getLeague().getId().equals(canonicalLeagueId)) {
      throw new IllegalArgumentException(
          "Canonical season does not belong to the selected SofaScore tournament");
    }
    identities.register(
        ResourceType.SEASON, PROVIDER, Long.toString(sofaScoreSeasonId), canonicalSeasonId);

    List<SofaScoreEventSnapshot> events =
        provider.fetchEvents(tournamentId, sofaScoreSeasonId, window, page);
    List<String> warnings = new ArrayList<>();
    int created = 0;
    int updated = 0;
    int skipped = 0;
    for (SofaScoreEventSnapshot event : events) {
      Optional<Long> homeTeamId =
          identities.findCanonicalId(ResourceType.TEAM, PROVIDER, event.homeTeamExternalId());
      Optional<Long> awayTeamId =
          identities.findCanonicalId(ResourceType.TEAM, PROVIDER, event.awayTeamExternalId());
      if (homeTeamId.isEmpty() || awayTeamId.isEmpty()) {
        warnings.add(
            "Skipped SofaScore event "
                + event.externalId()
                + ": one or both teams are not mapped to canonical teams.");
        skipped++;
        continue;
      }
      boolean inserted =
          matches.upsertFromProvider(
              PROVIDER,
              event.externalId(),
              canonicalSeasonId,
              homeTeamId.get(),
              awayTeamId.get(),
              event.kickoffAt(),
              normalizeStatus(event.statusCode()),
              event.homeScore(),
              event.awayScore(),
              event.roundName());
      if (inserted) created++;
      else updated++;
    }
    return new MatchSyncResult(events.size(), created, updated, skipped, List.copyOf(warnings));
  }

  private MatchStatus normalizeStatus(String statusCode) {
    if (statusCode == null) return MatchStatus.UNKNOWN;
    return switch (statusCode.toLowerCase(Locale.ROOT)) {
      case "notstarted" -> MatchStatus.NOT_STARTED;
      case "inprogress" -> MatchStatus.IN_PROGRESS;
      case "finished", "afterextra", "afterpenalties" -> MatchStatus.FINISHED;
      case "postponed" -> MatchStatus.POSTPONED;
      case "canceled", "cancelled" -> MatchStatus.CANCELED;
      case "suspended" -> MatchStatus.SUSPENDED;
      case "abandoned" -> MatchStatus.ABANDONED;
      default -> MatchStatus.UNKNOWN;
    };
  }

  public record MatchSyncResult(
      int discoveredEvents,
      int createdMatches,
      int updatedMatches,
      int skippedEvents,
      List<String> warnings) {}
}
