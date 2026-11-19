package com.h2football.module.analytics.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ModelEvaluationCalculator {
  private static final int SCALE = 10;
  private static final double MIN_PROBABILITY = 1.0e-15;

  public EvaluationMetrics calculate(
      BigDecimal homeProbability,
      BigDecimal drawProbability,
      BigDecimal awayProbability,
      int actualHomeScore,
      int actualAwayScore) {
    if (homeProbability == null
        || drawProbability == null
        || awayProbability == null
        || actualHomeScore < 0
        || actualAwayScore < 0) {
      throw new IllegalArgumentException(
          "Three probabilities and non-negative scores are required");
    }

    List<BigDecimal> probabilities = List.of(homeProbability, drawProbability, awayProbability);
    if (probabilities.stream()
        .anyMatch(
            value -> value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
      throw new IllegalArgumentException("Probabilities must be between 0 and 1");
    }

    BigDecimal probabilityTotal = probabilities.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (probabilityTotal.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.000001")) > 0) {
      throw new IllegalArgumentException("Outcome probabilities must sum to 1");
    }

    int actualIndex =
        actualHomeScore > actualAwayScore ? 0 : actualHomeScore == actualAwayScore ? 1 : 2;
    BigDecimal brierScore = BigDecimal.ZERO;
    for (int index = 0; index < probabilities.size(); index++) {
      BigDecimal observed = index == actualIndex ? BigDecimal.ONE : BigDecimal.ZERO;
      BigDecimal error = probabilities.get(index).subtract(observed);
      brierScore = brierScore.add(error.multiply(error));
    }

    BigDecimal logLoss =
        BigDecimal.valueOf(
                -Math.log(Math.max(probabilities.get(actualIndex).doubleValue(), MIN_PROBABILITY)))
            .setScale(SCALE, RoundingMode.HALF_UP);
    return new EvaluationMetrics(
        brierScore.setScale(SCALE, RoundingMode.HALF_UP),
        logLoss,
        actualIndex == 0 ? "HOME" : actualIndex == 1 ? "DRAW" : "AWAY",
        probabilities.get(actualIndex));
  }

  public record EvaluationMetrics(
      BigDecimal brierScore,
      BigDecimal logLoss,
      String actualOutcome,
      BigDecimal probabilityOfActualOutcome) {}
}
