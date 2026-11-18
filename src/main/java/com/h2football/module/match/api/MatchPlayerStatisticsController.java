package com.h2football.module.match.api;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.application.MatchPlayerStatisticsApplicationService;
import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/player-statistics")
public class MatchPlayerStatisticsController {
  private final MatchPlayerStatisticsApplicationService statistics;

  public MatchPlayerStatisticsController(MatchPlayerStatisticsApplicationService statistics) {
    this.statistics = statistics;
  }

  @GetMapping
  public List<PlayerStatisticResponse> find(
      @PathVariable Long matchId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return statistics.findByMatch(matchId, provider).stream()
        .map(PlayerStatisticResponse::from)
        .toList();
  }

  public record PlayerStatisticResponse(
      Long playerId,
      String playerName,
      Provider provider,
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
      String rawStatistics) {
    static PlayerStatisticResponse from(MatchPlayerStatistic item) {
      return new PlayerStatisticResponse(
          item.getPlayer().getId(),
          item.getPlayer().getName(),
          item.getProvider(),
          item.getRating(),
          item.getMinutesPlayed(),
          item.getGoals(),
          item.getAssists(),
          item.getTotalShots(),
          item.getShotsOnTarget(),
          item.getAccuratePasses(),
          item.getTotalPasses(),
          item.getKeyPasses(),
          item.getTackles(),
          item.getInterceptions(),
          item.getClearances(),
          item.getExpectedGoals(),
          item.getExpectedAssists(),
          item.getRawStatistics());
    }
  }
}
