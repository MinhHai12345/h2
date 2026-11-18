package com.h2football.module.match.api;

import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import java.time.Instant;

public final class MatchDtos {
  private MatchDtos() {}

  public record MatchResponse(
      Long id,
      Long seasonId,
      String seasonName,
      String leagueName,
      Long homeTeamId,
      String homeTeamName,
      Long awayTeamId,
      String awayTeamName,
      Instant kickoffAt,
      MatchStatus status,
      Integer homeScore,
      Integer awayScore,
      String roundName) {
    public static MatchResponse from(Match match) {
      return new MatchResponse(
          match.getId(),
          match.getSeason().getId(),
          match.getSeason().getDisplayName(),
          match.getSeason().getLeague().getName(),
          match.getHomeTeam().getId(),
          match.getHomeTeam().getName(),
          match.getAwayTeam().getId(),
          match.getAwayTeam().getName(),
          match.getKickoffAt(),
          match.getStatus(),
          match.getHomeScore(),
          match.getAwayScore(),
          match.getRoundName());
    }
  }
}
