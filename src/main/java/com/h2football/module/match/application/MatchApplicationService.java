package com.h2football.module.match.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.application.MasterDataNotFoundException;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchApplicationService {
  private final MatchRepository matches;
  private final SeasonRepository seasons;
  private final TeamRepository teams;
  private final ExternalIdentityService identities;

  public MatchApplicationService(
      MatchRepository matches,
      SeasonRepository seasons,
      TeamRepository teams,
      ExternalIdentityService identities) {
    this.matches = matches;
    this.seasons = seasons;
    this.teams = teams;
    this.identities = identities;
  }

  public boolean upsertFromProvider(
      Provider provider,
      String externalId,
      Long seasonId,
      Long homeTeamId,
      Long awayTeamId,
      Instant kickoffAt,
      MatchStatus status,
      Integer homeScore,
      Integer awayScore,
      String roundName) {
    if (provider == null) throw new IllegalArgumentException("provider must not be null");
    if (externalId == null || externalId.isBlank())
      throw new IllegalArgumentException("externalId must not be blank");
    if (seasonId == null || seasonId <= 0)
      throw new IllegalArgumentException("seasonId must be positive");
    if (homeTeamId == null || awayTeamId == null || Objects.equals(homeTeamId, awayTeamId)) {
      throw new IllegalArgumentException("Two different canonical teams are required");
    }

    Long existingId =
        identities.findCanonicalId(ResourceType.MATCH, provider, externalId.trim()).orElse(null);
    if (existingId != null) {
      Match existing =
          matches
              .findById(existingId)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Match identity references missing canonical ID " + existingId));
      if (!Objects.equals(existing.getSeason().getId(), seasonId)
          || !Objects.equals(existing.getHomeTeam().getId(), homeTeamId)
          || !Objects.equals(existing.getAwayTeam().getId(), awayTeamId)) {
        throw new IllegalStateException(
            "Provider match identity conflicts with canonical season or participants");
      }
      existing.updateSnapshot(kickoffAt, status, homeScore, awayScore, roundName);
      return false;
    }

    Season season =
        seasons
            .findById(seasonId)
            .orElseThrow(() -> new MasterDataNotFoundException("Season", seasonId));
    Team homeTeam =
        teams
            .findById(homeTeamId)
            .orElseThrow(() -> new MasterDataNotFoundException("Team", homeTeamId));
    Team awayTeam =
        teams
            .findById(awayTeamId)
            .orElseThrow(() -> new MasterDataNotFoundException("Team", awayTeamId));
    Match match =
        matches.save(
            new Match(
                season, homeTeam, awayTeam, kickoffAt, status, homeScore, awayScore, roundName));
    identities.register(ResourceType.MATCH, provider, externalId.trim(), match.getId());
    return true;
  }

  @Transactional(readOnly = true)
  public List<Match> findBySeason(Long seasonId) {
    if (seasonId == null || seasonId <= 0)
      throw new IllegalArgumentException("seasonId must be positive");
    return matches.findAllBySeasonIdOrderByKickoffAtAsc(seasonId);
  }

  @Transactional(readOnly = true)
  public Match get(Long id) {
    return matches.findById(id).orElseThrow(() -> new MatchNotFoundException(id));
  }
}
