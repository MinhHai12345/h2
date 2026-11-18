package com.h2football.module.analytics.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.model.MatchStatistic;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeamMatchStatisticsApplicationService {
  private final MatchRepository matches;
  private final TeamRepository teams;
  private final MatchStatisticRepository statistics;

  public TeamMatchStatisticsApplicationService(
      MatchRepository matches, TeamRepository teams, MatchStatisticRepository statistics) {
    this.matches = matches;
    this.teams = teams;
    this.statistics = statistics;
  }

  public List<TeamMatchStatisticResponse> findByMatch(
      Long teamId, Long matchId, Provider provider) {
    if (teamId == null || teamId <= 0) {
      throw new IllegalArgumentException("teamId must be positive");
    }
    if (matchId == null || matchId <= 0) {
      throw new IllegalArgumentException("matchId must be positive");
    }
    if (provider == null) {
      throw new IllegalArgumentException("provider must not be null");
    }
    if (!teams.existsById(teamId)) {
      throw new IllegalArgumentException("Unknown team " + teamId);
    }
    var match =
        matches
            .findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown match " + matchId));
    boolean home = match.getHomeTeam().getId().equals(teamId);
    boolean away = match.getAwayTeam().getId().equals(teamId);
    if (!home && !away) {
      throw new IllegalArgumentException("Team does not participate in match " + matchId);
    }

    return statistics.findAllByMatchIdOrderByPeriodAscGroupNameAscStatisticKeyAsc(matchId).stream()
        .filter(item -> item.getProvider() == provider)
        .map(item -> toResponse(item, home))
        .toList();
  }

  private TeamMatchStatisticResponse toResponse(MatchStatistic item, boolean home) {
    return new TeamMatchStatisticResponse(
        item.getId(),
        item.getProvider(),
        item.getPeriod(),
        item.getGroupName(),
        item.getStatisticKey(),
        item.getStatisticName(),
        home ? item.getHomeValue() : item.getAwayValue(),
        home ? item.getAwayValue() : item.getHomeValue(),
        home ? item.getHomeDisplay() : item.getAwayDisplay(),
        home ? item.getAwayDisplay() : item.getHomeDisplay());
  }

  public record TeamMatchStatisticResponse(
      Long id,
      Provider provider,
      String period,
      String groupName,
      String statisticKey,
      String statisticName,
      BigDecimal teamValue,
      BigDecimal opponentValue,
      String teamDisplay,
      String opponentDisplay) {}
}
