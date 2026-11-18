package com.h2football.module.analytics.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MatchAnalyticsCalculator {
  private static final int SCALE = 8;
  private static final int MAX_GOALS = 10;
  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal MAX_EXPECTED_GOALS = new BigDecimal("15");

  public BigDecimal calculateEmaForm(List<Integer> pointsNewestFirst, int period) {
    if (pointsNewestFirst == null || period <= 0) {
      throw new IllegalArgumentException("Form observations and positive period are required");
    }
    if (pointsNewestFirst.isEmpty()) {
      return null;
    }
    if (pointsNewestFirst.stream().anyMatch(points -> points == null || points < 0 || points > 3)) {
      throw new IllegalArgumentException("Form points must be between 0 and 3");
    }
    BigDecimal alpha =
        BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1L), SCALE, RoundingMode.HALF_UP);
    BigDecimal ema = BigDecimal.valueOf(pointsNewestFirst.get(pointsNewestFirst.size() - 1));
    for (int index = pointsNewestFirst.size() - 2; index >= 0; index--) {
      ema =
          alpha
              .multiply(BigDecimal.valueOf(pointsNewestFirst.get(index)))
              .add(ONE.subtract(alpha).multiply(ema));
    }
    return ema.divide(BigDecimal.valueOf(3), SCALE, RoundingMode.HALF_UP);
  }

  public MatchProbabilities calculateProbabilities(
      BigDecimal homeExpectedGoals, BigDecimal awayExpectedGoals) {
    validateExpectedGoals(homeExpectedGoals, "homeExpectedGoals");
    validateExpectedGoals(awayExpectedGoals, "awayExpectedGoals");

    BigDecimal[] homeDistribution = poissonDistribution(homeExpectedGoals);
    BigDecimal[] awayDistribution = poissonDistribution(awayExpectedGoals);
    BigDecimal homeWin = BigDecimal.ZERO;
    BigDecimal draw = BigDecimal.ZERO;
    BigDecimal awayWin = BigDecimal.ZERO;
    BigDecimal over25 = BigDecimal.ZERO;

    for (int homeGoals = 0; homeGoals <= MAX_GOALS; homeGoals++) {
      for (int awayGoals = 0; awayGoals <= MAX_GOALS; awayGoals++) {
        BigDecimal probability = homeDistribution[homeGoals].multiply(awayDistribution[awayGoals]);
        if (homeGoals > awayGoals) homeWin = homeWin.add(probability);
        else if (homeGoals == awayGoals) draw = draw.add(probability);
        else awayWin = awayWin.add(probability);
        if (homeGoals + awayGoals > 2) over25 = over25.add(probability);
      }
    }

    BigDecimal bothTeamsScore =
        ONE.subtract(homeDistribution[0]).multiply(ONE.subtract(awayDistribution[0]));
    BigDecimal total = homeWin.add(draw).add(awayWin);
    BigDecimal normalizedHomeWin = normalize(homeWin, total);
    BigDecimal normalizedDraw = normalize(draw, total);
    BigDecimal normalizedAwayWin =
        ONE.subtract(normalizedHomeWin)
            .subtract(normalizedDraw)
            .setScale(SCALE, RoundingMode.HALF_UP);
    return new MatchProbabilities(
        normalizedHomeWin,
        normalizedDraw,
        normalizedAwayWin,
        normalize(over25, total),
        bothTeamsScore.setScale(SCALE, RoundingMode.HALF_UP));
  }

  public BigDecimal calculateMatchAttractivenessIndex(
      BigDecimal homeExpectedGoals, BigDecimal awayExpectedGoals) {
    validateExpectedGoals(homeExpectedGoals, "homeExpectedGoals");
    validateExpectedGoals(awayExpectedGoals, "awayExpectedGoals");
    BigDecimal total = homeExpectedGoals.add(awayExpectedGoals);
    BigDecimal intensity =
        total.divide(BigDecimal.valueOf(4), SCALE, RoundingMode.HALF_UP).min(ONE);
    BigDecimal imbalance =
        homeExpectedGoals
            .subtract(awayExpectedGoals)
            .abs()
            .divide(new BigDecimal("2.5"), SCALE, RoundingMode.HALF_UP)
            .min(ONE);
    BigDecimal balance = ONE.subtract(imbalance);
    return intensity
        .multiply(new BigDecimal("0.65"))
        .add(balance.multiply(new BigDecimal("0.35")))
        .multiply(HUNDRED)
        .setScale(3, RoundingMode.HALF_UP);
  }

  public BigDecimal adjustExpectedGoals(
      BigDecimal baseExpectedGoals, BigDecimal teamForm, BigDecimal opponentForm) {
    validateExpectedGoals(baseExpectedGoals, "baseExpectedGoals");
    if (teamForm == null || opponentForm == null) {
      return baseExpectedGoals.setScale(6, RoundingMode.HALF_UP);
    }
    BigDecimal adjustment =
        ONE.add(new BigDecimal("0.15").multiply(teamForm.subtract(opponentForm)));
    return baseExpectedGoals
        .multiply(adjustment)
        .max(BigDecimal.ZERO)
        .min(MAX_EXPECTED_GOALS)
        .setScale(6, RoundingMode.HALF_UP);
  }

  private BigDecimal[] poissonDistribution(BigDecimal expectedGoals) {
    double lambda = expectedGoals.doubleValue();
    double[] values = new double[MAX_GOALS + 1];
    values[0] = Math.exp(-lambda);
    for (int goals = 1; goals <= MAX_GOALS; goals++) {
      values[goals] = values[goals - 1] * lambda / goals;
    }
    double sum = 0;
    for (double value : values) sum += value;
    BigDecimal[] result = new BigDecimal[MAX_GOALS + 1];
    for (int goals = 0; goals <= MAX_GOALS; goals++) {
      result[goals] = BigDecimal.valueOf(values[goals] / sum).setScale(SCALE, RoundingMode.HALF_UP);
    }
    return result;
  }

  private BigDecimal normalize(BigDecimal value, BigDecimal total) {
    return total.signum() == 0
        ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
        : value.divide(total, SCALE, RoundingMode.HALF_UP);
  }

  private void validateExpectedGoals(BigDecimal value, String field) {
    if (value == null || value.signum() < 0 || value.compareTo(MAX_EXPECTED_GOALS) > 0) {
      throw new IllegalArgumentException(field + " must be between 0 and 15");
    }
  }

  public record MatchProbabilities(
      BigDecimal homeWin,
      BigDecimal draw,
      BigDecimal awayWin,
      BigDecimal over25,
      BigDecimal bothTeamsScore) {}
}
