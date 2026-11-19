package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchAnalyticsCalculatorTest {
  private final MatchAnalyticsCalculator calculator = new MatchAnalyticsCalculator();

  @Test
  void calculatesEmaWithHigherWeightForLatestResult() {
    BigDecimal result = calculator.calculateEmaForm(List.of(3, 0, 1), 5);

    assertThat(result).isEqualByComparingTo("0.48148148");
  }

  @Test
  void poissonProbabilitiesAreNormalizedAndBounded() {
    var result = calculator.calculateProbabilities(new BigDecimal("1.55"), new BigDecimal("1.10"));

    assertThat(result.homeWin().add(result.draw()).add(result.awayWin()))
        .isEqualByComparingTo("1.00000000");
    assertThat(result.homeWin()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
    assertThat(result.draw()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
    assertThat(result.awayWin()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
    assertThat(result.over25()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
    assertThat(result.bothTeamsScore()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
  }

  @Test
  void adjustsExpectedGoalsUsingBoundedLineupImpactAndCoverage() {
    BigDecimal boosted =
        calculator.adjustExpectedGoalsByPlayerImpact(
            new BigDecimal("1.500000"),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            new BigDecimal("100"));
    BigDecimal reduced =
        calculator.adjustExpectedGoalsByPlayerImpact(
            new BigDecimal("1.500000"),
            BigDecimal.ZERO,
            BigDecimal.ONE,
            new BigDecimal("100"));
    BigDecimal partial =
        calculator.adjustExpectedGoalsByPlayerImpact(
            new BigDecimal("1.500000"),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            new BigDecimal("40"));

    assertThat(boosted).isEqualByComparingTo("1.575000");
    assertThat(reduced).isEqualByComparingTo("1.425000");
    assertThat(partial).isEqualByComparingTo("1.530000");
  }

  @Test
  void leavesExpectedGoalsUnchangedWhenPlayerImpactIsMissing() {
    BigDecimal result =
        calculator.adjustExpectedGoalsByPlayerImpact(
            new BigDecimal("1.500000"), null, BigDecimal.ZERO, BigDecimal.ZERO);

    assertThat(result).isEqualByComparingTo("1.500000");
  }

  @Test
  void rejectsOutOfRangePlayerImpactIndicesAndCoverage() {
    assertThatThrownBy(
            () ->
                calculator.adjustExpectedGoalsByPlayerImpact(
                    BigDecimal.ONE,
                    new BigDecimal("1.1"),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(100)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                calculator.adjustExpectedGoalsByPlayerImpact(
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    new BigDecimal("101")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void calculatesAttractivenessFromGoalIntensityAndCompetitiveBalance() {
    BigDecimal result =
        calculator.calculateMatchAttractivenessIndex(
            new BigDecimal("2.00"), new BigDecimal("2.00"));

    assertThat(result).isEqualByComparingTo("100.000");
  }

  @Test
  void adjustsExpectedGoalsByRelativeEmaForm() {
    BigDecimal result =
        calculator.adjustExpectedGoals(
            new BigDecimal("1.500000"), new BigDecimal("0.800000"), new BigDecimal("0.400000"));

    assertThat(result).isEqualByComparingTo("1.590000");
  }

  @Test
  void rejectsInvalidFormPointsAndExpectedGoals() {
    assertThatThrownBy(() -> calculator.calculateEmaForm(List.of(4, 1), 5))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> calculator.calculateProbabilities(new BigDecimal("-0.1"), new BigDecimal("1.0")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
