package com.h2football.module.analytics.application;

import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PlayerPerformanceCalculator {
  private static final BigDecimal NINETY = BigDecimal.valueOf(90);
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final int SCALE = 3;

  public PlayerPerformance calculate(PlayerSeasonStatistic statistic) {
    if (statistic == null) {
      throw new IllegalArgumentException("Player season statistic is required");
    }
    Long minutes = statistic.getMinutesPlayed();
    Long goals = statistic.getGoals();
    Long assists = statistic.getAssists();
    Long shots = statistic.getTotalShots();
    Long shotsOnTarget = statistic.getShotsOnTarget();
    Long accuratePasses = statistic.getAccuratePasses();
    Long totalPasses = statistic.getTotalPasses();
    Long keyPasses = statistic.getKeyPasses();
    long defensiveActions =
        statistic.getTackles() + statistic.getInterceptions() + statistic.getClearances();

    return new PlayerPerformance(
        perNinety(goals, minutes),
        perNinety(assists, minutes),
        perNinety(goals + assists, minutes),
        perNinety(statistic.getExpectedGoals(), minutes),
        perNinety(statistic.getExpectedAssists(), minutes),
        perNinety(shots, minutes),
        perNinety(keyPasses, minutes),
        perNinety(defensiveActions, minutes),
        percentage(shotsOnTarget, shots),
        percentage(accuratePasses, totalPasses),
        percentage(goals, shots),
        difference(goals, statistic.getExpectedGoals()));
  }

  private BigDecimal perNinety(Long value, Long minutes) {
    return minutes == null || minutes <= 0 || value == null
        ? null
        : BigDecimal.valueOf(value)
            .multiply(NINETY)
            .divide(BigDecimal.valueOf(minutes), SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal perNinety(BigDecimal value, Long minutes) {
    return minutes == null || minutes <= 0 || value == null
        ? null
        : value.multiply(NINETY).divide(BigDecimal.valueOf(minutes), SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal percentage(Long numerator, Long denominator) {
    return numerator == null || denominator == null || denominator <= 0
        ? null
        : BigDecimal.valueOf(numerator)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(denominator), SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal difference(Long actual, BigDecimal expected) {
    return actual == null || expected == null
        ? null
        : BigDecimal.valueOf(actual).subtract(expected).setScale(SCALE, RoundingMode.HALF_UP);
  }

  public record PlayerPerformance(
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
