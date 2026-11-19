package com.h2football.module.analytics.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.LineupRole;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchLineup;
import com.h2football.module.match.domain.model.MatchLineupPlayer;
import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchLineupRepository;
import com.h2football.module.match.domain.repository.MatchPlayerStatisticRepository;
import com.h2football.module.match.domain.repository.MatchRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayerImpactApplicationService {
  private static final int SCALE = 3;

  private final MatchRepository matches;
  private final MatchLineupRepository lineups;
  private final MatchPlayerStatisticRepository playerStatistics;
  private final PlayerImpactCalculator calculator;

  public PlayerImpactApplicationService(
      MatchRepository matches,
      MatchLineupRepository lineups,
      MatchPlayerStatisticRepository playerStatistics,
      PlayerImpactCalculator calculator) {
    this.matches = matches;
    this.lineups = lineups;
    this.playerStatistics = playerStatistics;
    this.calculator = calculator;
  }

  public MatchPlayerImpactResponse calculate(Long matchId, Provider provider) {
    if (matchId == null || matchId <= 0 || provider == null) {
      throw new IllegalArgumentException("Positive matchId and provider are required");
    }
    Match match =
        matches
            .findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown match " + matchId));
    List<MatchLineup> matchLineups =
        lineups.findAllByMatchIdAndProviderOrderByTeamIdAsc(matchId, provider);
    Map<Long, MatchLineup> confirmedLineups = new HashMap<>();
    matchLineups.stream()
        .filter(MatchLineup::isConfirmed)
        .forEach(lineup -> confirmedLineups.put(lineup.getTeam().getId(), lineup));

    List<Long> playerIds =
        confirmedLineups.values().stream()
            .flatMap(lineup -> lineup.getPlayers().stream())
            .filter(player -> player.getRole() == LineupRole.STARTER)
            .map(player -> player.getPlayer().getId())
            .distinct()
            .toList();

    Map<Long, List<MatchPlayerStatistic>> historyByPlayer = new HashMap<>();
    if (!playerIds.isEmpty()) {
      playerStatistics
          .findHistoryBefore(
              match.getSeason().getId(),
              match.getKickoffAt(),
              MatchStatus.FINISHED,
              provider,
              playerIds)
          .forEach(
              statistic ->
                  historyByPlayer
                      .computeIfAbsent(statistic.getPlayer().getId(), ignored -> new ArrayList<>())
                      .add(statistic));
    }

    TeamImpact home =
        buildTeamImpact(
            match.getHomeTeam().getId(),
            match.getHomeTeam().getName(),
            confirmedLineups.get(match.getHomeTeam().getId()),
            historyByPlayer);
    TeamImpact away =
        buildTeamImpact(
            match.getAwayTeam().getId(),
            match.getAwayTeam().getName(),
            confirmedLineups.get(match.getAwayTeam().getId()),
            historyByPlayer);
    return new MatchPlayerImpactResponse(
        matchId, provider, match.getKickoffAt(), home, away, "player-impact-v1");
  }

  private TeamImpact buildTeamImpact(
      Long teamId,
      String teamName,
      MatchLineup lineup,
      Map<Long, List<MatchPlayerStatistic>> historyByPlayer) {
    if (lineup == null) {
      return new TeamImpact(teamId, teamName, false, null, 0, 0, null, null, List.of());
    }
    List<PlayerImpactResponse> players =
        lineup.getPlayers().stream()
            .filter(player -> player.getRole() == LineupRole.STARTER)
            .map(
                player ->
                    toPlayerResponse(
                        player,
                        historyByPlayer.getOrDefault(player.getPlayer().getId(), List.of())))
            .sorted(
                Comparator.comparing(
                    PlayerImpactResponse::impactScore,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    List<BigDecimal> availableScores =
        players.stream()
            .map(PlayerImpactResponse::impactScore)
            .filter(java.util.Objects::nonNull)
            .toList();
    BigDecimal average =
        availableScores.isEmpty()
            ? null
            : availableScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(availableScores.size()), SCALE, RoundingMode.HALF_UP);
    BigDecimal coverage =
        players.isEmpty()
            ? BigDecimal.ZERO.setScale(SCALE)
            : BigDecimal.valueOf(availableScores.size())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(players.size()), SCALE, RoundingMode.HALF_UP);
    return new TeamImpact(
        teamId,
        teamName,
        true,
        lineup.getFormation(),
        players.size(),
        availableScores.size(),
        average,
        coverage,
        players);
  }

  private PlayerImpactResponse toPlayerResponse(
      MatchLineupPlayer lineupPlayer, List<MatchPlayerStatistic> history) {
    String position =
        lineupPlayer.getPosition() == null
            ? lineupPlayer.getPlayer().getPosition()
            : lineupPlayer.getPosition();
    PlayerImpactCalculator.PlayerImpact impact = calculator.calculate(history, position);
    return new PlayerImpactResponse(
        lineupPlayer.getPlayer().getId(),
        lineupPlayer.getPlayer().getName(),
        position,
        lineupPlayer.isCaptain(),
        impact.score(),
        impact.attackingIndex(),
        impact.creationIndex(),
        impact.defensiveIndex(),
        impact.averageRating(),
        impact.minutesObserved(),
        impact.matchesObserved());
  }

  public record MatchPlayerImpactResponse(
      Long matchId,
      Provider provider,
      java.time.Instant kickoffAt,
      TeamImpact home,
      TeamImpact away,
      String modelVersion) {}

  public record TeamImpact(
      Long teamId,
      String teamName,
      boolean lineupConfirmed,
      String formation,
      int startingPlayers,
      int playersWithHistoricalData,
      BigDecimal averageStartingImpact,
      BigDecimal dataCoveragePercent,
      List<PlayerImpactResponse> players) {}

  public record PlayerImpactResponse(
      Long playerId,
      String playerName,
      String position,
      boolean captain,
      BigDecimal impactScore,
      BigDecimal attackingIndex,
      BigDecimal creationIndex,
      BigDecimal defensiveIndex,
      BigDecimal averageRating,
      long minutesObserved,
      int matchesObserved) {}
}
