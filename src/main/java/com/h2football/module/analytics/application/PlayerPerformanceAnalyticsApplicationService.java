package com.h2football.module.analytics.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayerPerformanceAnalyticsApplicationService {
  private final PlayerSeasonStatisticsApplicationService seasonStatistics;
  private final PlayerPerformanceCalculator calculator;

  public PlayerPerformanceAnalyticsApplicationService(
      PlayerSeasonStatisticsApplicationService seasonStatistics,
      PlayerPerformanceCalculator calculator) {
    this.seasonStatistics = seasonStatistics;
    this.calculator = calculator;
  }

  public List<PlayerPerformanceResponse> findBySeason(
      Long seasonId, Provider provider, long minimumMinutes, String position) {
    if (minimumMinutes < 0) {
      throw new IllegalArgumentException("minimumMinutes must not be negative");
    }
    return seasonStatistics.findBySeason(seasonId, provider).stream()
        .filter(item -> item.getMinutesPlayed() >= minimumMinutes)
        .filter(item -> matchesPosition(item, position))
        .map(this::toResponse)
        .sorted(
            Comparator.comparing(
                    PlayerPerformanceResponse::goalContributionsPer90,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                    PlayerPerformanceResponse::averageRating,
                    Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  private boolean matchesPosition(PlayerSeasonStatistic statistic, String position) {
    return position == null
        || position.isBlank()
        || position.trim().equalsIgnoreCase(statistic.getPlayer().getPosition());
  }

  private PlayerPerformanceResponse toResponse(PlayerSeasonStatistic statistic) {
    var metrics = calculator.calculate(statistic);
    return new PlayerPerformanceResponse(
        statistic.getPlayer().getId(),
        statistic.getPlayer().getName(),
        statistic.getPlayer().getPosition(),
        statistic.getProvider(),
        statistic.getAppearances(),
        statistic.getMinutesPlayed(),
        statistic.getGoals(),
        statistic.getAssists(),
        statistic.getExpectedGoals(),
        statistic.getExpectedAssists(),
        statistic.getAverageRating(),
        metrics.goalsPer90(),
        metrics.assistsPer90(),
        metrics.goalContributionsPer90(),
        metrics.expectedGoalsPer90(),
        metrics.expectedAssistsPer90(),
        metrics.shotsPer90(),
        metrics.keyPassesPer90(),
        metrics.defensiveActionsPer90(),
        metrics.shotAccuracyPercent(),
        metrics.passAccuracyPercent(),
        metrics.goalConversionPercent(),
        metrics.goalsMinusExpectedGoals());
  }

  public record PlayerPerformanceResponse(
      Long playerId,
      String playerName,
      String position,
      Provider provider,
      Long appearances,
      Long minutesPlayed,
      Long goals,
      Long assists,
      BigDecimal expectedGoals,
      BigDecimal expectedAssists,
      BigDecimal averageRating,
      BigDecimal goalsPer90,
      BigDecimal assistsPer90,
      BigDecimal goalContributionsPer90,
      BigDecimal expectedGoalsPer90,
      BigDecimal expectedAssistsPer90,
      BigDecimal shotsPer90,
      BigDecimal keyPassesPer90,
      BigDecimal defensiveActionsPer90,
      BigDecimal shotAccuracyPercent,
      BigDecimal passAccuracyPercent,
      BigDecimal goalConversionPercent,
      BigDecimal goalsMinusExpectedGoals) {}
}
