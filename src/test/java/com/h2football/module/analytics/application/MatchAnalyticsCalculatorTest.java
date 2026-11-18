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
