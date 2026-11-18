package com.h2football.module.match.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import com.h2football.module.match.domain.model.MatchPlayerStatistic.MatchPlayerStatisticSnapshot;
import com.h2football.module.match.domain.repository.MatchPlayerStatisticRepository;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchPlayerStatisticsApplicationService {
  private final MatchRepository matches;
  private final PlayerRepository players;
  private final MatchPlayerStatisticRepository statistics;

  public MatchPlayerStatisticsApplicationService(
      MatchRepository matches,
      PlayerRepository players,
      MatchPlayerStatisticRepository statistics) {
    this.matches = matches;
    this.players = players;
    this.statistics = statistics;
  }

  public int upsertFromProvider(
      Long matchId, Provider provider, List<PlayerStatisticInput> snapshots) {
    if (matchId == null || matchId <= 0) {
      throw new IllegalArgumentException("matchId must be positive");
    }
    if (provider == null) {
      throw new IllegalArgumentException("provider must not be null");
    }
    if (!matches.existsById(matchId)) {
      throw new MatchNotFoundException(matchId);
    }
    if (snapshots == null || snapshots.isEmpty()) {
      return 0;
    }

    Map<Long, PlayerStatisticInput> unique = new LinkedHashMap<>();
    for (PlayerStatisticInput item : snapshots) {
      if (item == null || item.playerId() == null || item.playerId() <= 0) {
        throw new IllegalArgumentException("Every player statistic must reference a player");
      }
      unique.put(item.playerId(), item);
    }
    if (unique.isEmpty()) {
      return 0;
    }

    var match = matches.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    int stored = 0;
    for (PlayerStatisticInput item : unique.values()) {
      var player =
          players
              .findById(item.playerId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException("Unknown canonical player " + item.playerId()));
      MatchPlayerStatisticSnapshot snapshot =
          new MatchPlayerStatisticSnapshot(
              item.rating(),
              item.minutesPlayed(),
              item.goals(),
              item.assists(),
              item.totalShots(),
              item.shotsOnTarget(),
              item.accuratePasses(),
              item.totalPasses(),
              item.keyPasses(),
              item.tackles(),
              item.interceptions(),
              item.clearances(),
              item.expectedGoals(),
              item.expectedAssists(),
              item.rawStatistics());
      MatchPlayerStatistic statistic =
          statistics
              .findByMatchIdAndPlayerIdAndProvider(matchId, item.playerId(), provider)
              .orElseGet(() -> new MatchPlayerStatistic(match, player, provider, snapshot));
      if (statistic.getId() != null) {
        statistic.updateSnapshot(snapshot);
      }
      statistics.save(statistic);
      stored++;
    }
    return stored;
  }

  @Transactional(readOnly = true)
  public List<MatchPlayerStatistic> findByMatch(Long matchId, Provider provider) {
    if (matchId == null || matchId <= 0) {
      throw new IllegalArgumentException("matchId must be positive");
    }
    if (provider == null) {
      throw new IllegalArgumentException("provider must not be null");
    }
    if (!matches.existsById(matchId)) {
      throw new MatchNotFoundException(matchId);
    }
    return statistics.findAllByMatchIdAndProviderOrderByPlayerNameAsc(matchId, provider);
  }

  public record PlayerStatisticInput(
      Long playerId,
      BigDecimal rating,
      Integer minutesPlayed,
      Integer goals,
      Integer assists,
      Integer totalShots,
      Integer shotsOnTarget,
      Integer accuratePasses,
      Integer totalPasses,
      Integer keyPasses,
      Integer tackles,
      Integer interceptions,
      Integer clearances,
      BigDecimal expectedGoals,
      BigDecimal expectedAssists,
      String rawStatistics) {}
}
