package com.h2football.module.ingestion.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.match.application.MatchStatisticsApplicationService;
import com.h2football.module.match.application.MatchStatisticsApplicationService.StatisticInput;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.provider.MultiProviderSeasonDataClient.ProviderMatchRecord;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderSeasonPersistenceService {
  private static final Duration MATCH_IDENTITY_TOLERANCE = Duration.ofHours(36);

  private final SeasonRepository seasons;
  private final TeamRepository teams;
  private final MatchRepository matches;
  private final ExternalIdentityService identities;
  private final MatchStatisticsApplicationService statistics;

  public ProviderSeasonPersistenceService(
      SeasonRepository seasons,
      TeamRepository teams,
      MatchRepository matches,
      ExternalIdentityService identities,
      MatchStatisticsApplicationService statistics) {
    this.seasons = seasons;
    this.teams = teams;
    this.matches = matches;
    this.identities = identities;
    this.statistics = statistics;
  }

  @Transactional
  public ProviderSeasonSyncResult persist(
      Provider provider, long seasonId, List<ProviderMatchRecord> records) {
    Season season =
        seasons
            .findById(seasonId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown canonical season " + seasonId));
    List<Team> knownTeams = new ArrayList<>(teams.findAll());
    List<Match> canonicalMatches =
        new ArrayList<>(matches.findAllBySeasonIdOrderByKickoffAtAsc(seasonId));
    int created = 0;
    int updated = 0;
    int linked = 0;
    int skipped = 0;

    for (ProviderMatchRecord record : records) {
      if (record == null
          || record.kickoffAt() == null
          || record.kickoffAt().equals(java.time.Instant.EPOCH)
          || record.externalMatchId() == null
          || record.externalMatchId().isBlank()
          || record.homeTeamName() == null
          || record.awayTeamName() == null) {
        skipped++;
        continue;
      }

      Team home =
          resolveTeam(
              season, provider, record.homeTeamExternalId(), record.homeTeamName(), knownTeams);
      Team away =
          resolveTeam(
              season, provider, record.awayTeamExternalId(), record.awayTeamName(), knownTeams);
      if (home.getId().equals(away.getId())) {
        skipped++;
        continue;
      }

      var providerMatchIdentity =
          identities.findCanonicalId(ResourceType.MATCH, provider, record.externalMatchId());
      Match match =
          providerMatchIdentity
              .map(
                  canonicalId ->
                      matches
                          .findById(canonicalId)
                          .orElseThrow(
                              () ->
                                  new IllegalStateException(
                                      "External match identity points to missing canonical match "
                                          + canonicalId)))
              .orElse(null);
      if (match != null && !match.getSeason().getId().equals(seasonId)) {
        throw new IllegalStateException(
            "Provider match identity is already assigned to another canonical season: "
                + record.externalMatchId());
      }
      boolean linkedFromAnotherProvider = false;

      if (match == null) {
        List<Match> candidates =
            canonicalMatches.stream()
                .filter(candidate -> candidate.getHomeTeam().getId().equals(home.getId()))
                .filter(candidate -> candidate.getAwayTeam().getId().equals(away.getId()))
                .filter(
                    candidate ->
                        Duration.between(record.kickoffAt(), candidate.getKickoffAt())
                            .abs()
                            .compareTo(MATCH_IDENTITY_TOLERANCE)
                            <= 0)
                .toList();
        if (candidates.size() > 1) {
          throw new IllegalStateException(
              "Ambiguous canonical match identity for provider "
                  + provider
                  + ", externalMatchId="
                  + record.externalMatchId());
        }
        if (candidates.size() == 1) {
          match = candidates.get(0);
          linkedFromAnotherProvider = true;
          identities.register(
              ResourceType.MATCH, provider, record.externalMatchId(), match.getId());
          linked++;
        }
      }

      MatchStatus status = mapStatus(record.status());
      if (match == null) {
        match =
            matches.save(
                new Match(
                    season,
                    home,
                    away,
                    record.kickoffAt(),
                    status,
                    record.homeScore(),
                    record.awayScore(),
                    record.roundName()));
        identities.register(ResourceType.MATCH, provider, record.externalMatchId(), match.getId());
        canonicalMatches.add(match);
        created++;
      } else {
        Integer homeScore =
            linkedFromAnotherProvider && match.getHomeScore() != null
                ? null
                : record.homeScore();
        Integer awayScore =
            linkedFromAnotherProvider && match.getAwayScore() != null
                ? null
                : record.awayScore();
        MatchStatus effectiveStatus =
            linkedFromAnotherProvider && match.getStatus() == MatchStatus.FINISHED
                ? MatchStatus.FINISHED
                : status;
        match.updateSnapshot(
            record.kickoffAt(), effectiveStatus, homeScore, awayScore, record.roundName());
        updated++;
      }
      persistExpectedGoals(match, provider, record);
    }

    return new ProviderSeasonSyncResult(
        provider, seasonId, records.size(), created, updated, linked, skipped);
  }

  private void persistExpectedGoals(
      Match match, Provider provider, ProviderMatchRecord record) {
    if (record.homeExpectedGoals() == null || record.awayExpectedGoals() == null) return;
    statistics.replaceFromProvider(
        match.getId(),
        provider,
        List.of(
            new StatisticInput(
                "ALL",
                "Expected Goals",
                "expectedGoals",
                "Expected Goals",
                record.homeExpectedGoals().toPlainString(),
                record.awayExpectedGoals().toPlainString(),
                record.homeExpectedGoals(),
                record.awayExpectedGoals())));
  }

  private Team resolveTeam(
      Season season,
      Provider provider,
      String externalId,
      String sourceName,
      List<Team> knownTeams) {
    String normalizedExternalId =
        externalId == null || externalId.isBlank() ? normalize(sourceName) : externalId.trim();
    var mapped =
        identities.findCanonicalId(ResourceType.TEAM, provider, normalizedExternalId);
    if (mapped.isPresent()) {
      return teams
          .findById(mapped.get())
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "External team identity points to missing canonical team " + mapped.get()));
    }

    String normalizedName = normalize(sourceName);
    List<Team> candidates =
        knownTeams.stream()
            .filter(team -> normalize(team.getName()).equals(normalizedName))
            .toList();
    if (candidates.size() > 1) {
      List<Team> leagueCountryCandidates =
          candidates.stream()
              .filter(
                  team ->
                      team.getCountry()
                          .getId()
                          .equals(season.getLeague().getCountry().getId()))
              .toList();
      if (leagueCountryCandidates.size() == 1) candidates = leagueCountryCandidates;
    }
    if (candidates.size() > 1) {
      throw new IllegalStateException(
          "Ambiguous team identity for provider " + provider + ": " + sourceName);
    }
    Team canonical =
        candidates.isEmpty()
            ? teams.save(new Team(sourceName.trim(), season.getLeague().getCountry()))
            : candidates.get(0);
    if (candidates.isEmpty()) knownTeams.add(canonical);
    identities.register(ResourceType.TEAM, provider, normalizedExternalId, canonical.getId());
    return canonical;
  }

  private static MatchStatus mapStatus(String sourceStatus) {
    if (sourceStatus == null) return MatchStatus.UNKNOWN;
    return switch (sourceStatus.trim().toUpperCase(Locale.ROOT)) {
      case "FINISHED", "AWARDED" -> MatchStatus.FINISHED;
      case "IN_PLAY", "LIVE", "PAUSED", "HALF_TIME", "1H", "2H" -> MatchStatus.IN_PROGRESS;
      case "POSTPONED" -> MatchStatus.POSTPONED;
      case "CANCELLED", "CANCELED" -> MatchStatus.CANCELED;
      case "SUSPENDED" -> MatchStatus.SUSPENDED;
      case "ABANDONED" -> MatchStatus.ABANDONED;
      case "SCHEDULED", "TIMED", "NOT_STARTED", "NS" -> MatchStatus.NOT_STARTED;
      default -> MatchStatus.UNKNOWN;
    };
  }

  private static String normalize(String value) {
    return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\b(football club|fc|afc|cf|sc|club)\\b", "")
        .replaceAll("[^a-z0-9]", "");
  }

  public record ProviderSeasonSyncResult(
      Provider provider,
      long seasonId,
      int discovered,
      int created,
      int updated,
      int linkedToExistingMatches,
      int skipped) {}
}
