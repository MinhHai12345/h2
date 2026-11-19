package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ModelEvaluationCalculatorTest {
  private final ModelEvaluationCalculator calculator = new ModelEvaluationCalculator();

  @Test
  void calculatesMulticlassBrierScoreAndLogLossForHomeWin() {
    var result =
        calculator.calculate(
            new BigDecimal("0.7"), new BigDecimal("0.2"), new BigDecimal("0.1"), 2, 0);

    assertThat(result.actualOutcome()).isEqualTo("HOME");
    assertThat(result.brierScore()).isEqualByComparingTo("0.1400000000");
    assertThat(result.logLoss()).isEqualByComparingTo("0.3566749439");
    assertThat(result.probabilityOfActualOutcome()).isEqualByComparingTo("0.7");
  }

  @Test
  void calculatesDrawAndAwayOutcomes() {
    var draw =
        calculator.calculate(
            new BigDecimal("0.2"), new BigDecimal("0.6"), new BigDecimal("0.2"), 1, 1);
    var away =
        calculator.calculate(
            new BigDecimal("0.2"), new BigDecimal("0.3"), new BigDecimal("0.5"), 0, 1);

    assertThat(draw.actualOutcome()).isEqualTo("DRAW");
    assertThat(draw.logLoss()).isEqualByComparingTo("0.5108256238");
    assertThat(away.actualOutcome()).isEqualTo("AWAY");
  }

  @Test
  void clipsZeroProbabilityToKeepLogLossFinite() {
    var result = calculator.calculate(new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO, 0, 1);

    assertThat(result.logLoss()).isGreaterThan(BigDecimal.ZERO);
    assertThat(result.logLoss()).isEqualByComparingTo("34.5387763949");
  }

  @Test
  void rejectsProbabilitiesThatDoNotSumToOne() {
    assertThatThrownBy(
            () ->
                calculator.calculate(
                    new BigDecimal("0.2"), new BigDecimal("0.2"), new BigDecimal("0.2"), 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeScoresAndProbabilitiesOutsideRange() {
    assertThatThrownBy(
            () ->
                calculator.calculate(new BigDecimal("1.1"), BigDecimal.ZERO, BigDecimal.ZERO, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> calculator.calculate(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, -1, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
