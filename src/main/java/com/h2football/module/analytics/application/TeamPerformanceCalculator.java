package com.h2football.module.analytics.application;

import com.h2football.module.analytics.domain.model.TeamSeasonStatistic;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TeamPerformanceCalculator {
  private static final int SCALE = 3;
  private static final String ALL_PERIOD = "ALL";

  public TeamPerformance calculate(
      List<TeamSeasonStatistic> statistics, BigDecimal expectedGoalsAgainst) {
    if (statistics == null) {
      throw new IllegalArgumentException("Team season statistics are required");
    }
    Map<String, BigDecimal> values =
        statistics.stream()
            .filter(item -> ALL_PERIOD.equalsIgnoreCase(item.getPeriod()))
            .filter(item -> item.getAverageValue() != null)
            .collect(
                Collectors.toMap(
                    item -> normalize(item.getStatisticKey()),
                    TeamSeasonStatistic::getAverageValue,
                    (first, second) -> first));

    BigDecimal expectedGoalsFor = find(values, "expectedgoals", "expected_goals");
    BigDecimal totalShots = find(values, "totalshots", "total_shots");
    BigDecimal shotsOnTarget = find(values, "shotsontarget", "shots_on_target");
    BigDecimal possession = find(values, "ballpossession", "ball_possession");

    return new TeamPerformance(
        expectedGoalsFor,
        expectedGoalsAgainst,
        difference(expectedGoalsFor, expectedGoalsAgainst),
        totalShots,
        shotsOnTarget,
        possession,
        percentage(shotsOnTarget, totalShots));
  }

  private BigDecimal find(Map<String, BigDecimal> values, String... keys) {
    for (String key : keys) {
      if (values.containsKey(key)) {
        return values.get(key);
      }
    }
    return null;
  }

  private String normalize(String key) {
    return key == null ? "" : key.replace("-", "").replace(" ", "").toLowerCase();
  }

  private BigDecimal difference(BigDecimal first, BigDecimal second) {
    return first == null || second == null
        ? null
        : first.subtract(second).setScale(SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
    return numerator == null || denominator == null || denominator.signum() <= 0
        ? null
        : numerator
            .multiply(BigDecimal.valueOf(100))
            .divide(denominator, SCALE, RoundingMode.HALF_UP);
  }

  public record TeamPerformance(
      BigDecimal expectedGoalsFor,
      BigDecimal expectedGoalsAgainst,
      BigDecimal expectedGoalsDifference,
      BigDecimal shotsPerMatch,
      BigDecimal shotsOnTargetPerMatch,
      BigDecimal possessionPercent,
      BigDecimal shotAccuracyPercent) {}
}
