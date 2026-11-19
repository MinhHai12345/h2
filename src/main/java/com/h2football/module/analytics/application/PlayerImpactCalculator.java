package com.h2football.module.analytics.application;

import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlayerImpactCalculator {
  private static final int HISTORY_LIMIT = 5;
  private static final int SCALE = 3;
  private static final BigDecimal NINETY = BigDecimal.valueOf(90);
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  public PlayerImpact calculate(List<MatchPlayerStatistic> observations, String position) {
    if (observations == null) {
      throw new IllegalArgumentException("Player observations are required");
    }
    List<MatchPlayerStatistic> recent = observations.stream().limit(HISTORY_LIMIT).toList();
    long minutes = sum(recent, MatchPlayerStatistic::getMinutesPlayed);
    if (minutes <= 0) {
      return new PlayerImpact(null, null, null, null, null, 0, recent.size());
    }

    BigDecimal goalsPer90 = per90(sum(recent, MatchPlayerStatistic::getGoals), minutes);
    BigDecimal assistsPer90 = per90(sum(recent, MatchPlayerStatistic::getAssists), minutes);
    BigDecimal shotsPer90 = per90(sum(recent, MatchPlayerStatistic::getTotalShots), minutes);
    BigDecimal keyPassesPer90 = per90(sum(recent, MatchPlayerStatistic::getKeyPasses), minutes);
    BigDecimal tacklesPer90 = per90(sum(recent, MatchPlayerStatistic::getTackles), minutes);
    BigDecimal interceptionsPer90 =
        per90(sum(recent, MatchPlayerStatistic::getInterceptions), minutes);
    BigDecimal clearancesPer90 = per90(sum(recent, MatchPlayerStatistic::getClearances), minutes);
    BigDecimal expectedGoalsPer90 = per90Decimal(recent, minutes, true);
    BigDecimal expectedAssistsPer90 = per90Decimal(recent, minutes, false);
    BigDecimal rating = averageRating(recent);

    BigDecimal attack =
        weighted(
            normalize(goalsPer90, "0.8"),
            new BigDecimal("0.35"),
            normalize(expectedGoalsPer90, "0.8"),
            new BigDecimal("0.35"),
            normalize(shotsPer90, "4"),
            new BigDecimal("0.30"));
    BigDecimal creation =
        weighted(
            normalize(assistsPer90, "0.4"),
            new BigDecimal("0.50"),
            normalize(expectedAssistsPer90, "0.4"),
            new BigDecimal("0.25"),
            normalize(keyPassesPer90, "2"),
            new BigDecimal("0.25"));
    BigDecimal defending =
        weighted(
            normalize(tacklesPer90, "3"),
            new BigDecimal("0.40"),
            normalize(interceptionsPer90, "2"),
            new BigDecimal("0.30"),
            normalize(clearancesPer90, "4"),
            new BigDecimal("0.30"));
    BigDecimal ratingIndex =
        rating == null
            ? null
            : rating
                .subtract(new BigDecimal("6"))
                .divide(new BigDecimal("2.5"), SCALE + 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);

    BigDecimal[] weights = weights(position);
    BigDecimal score =
        weighted(
            attack,
            weights[0],
            creation,
            weights[1],
            defending,
            weights[2],
            ratingIndex,
            weights[3]);
    return new PlayerImpact(
        score == null ? null : score.multiply(ONE_HUNDRED).setScale(SCALE, RoundingMode.HALF_UP),
        scale(attack),
        scale(creation),
        scale(defending),
        rating,
        minutes,
        recent.size());
  }

  private BigDecimal[] weights(String position) {
    String normalized = position == null ? "" : position.trim().toUpperCase();
    return switch (normalized) {
      case "F", "FW", "ST", "CF", "LW", "RW" ->
          new BigDecimal[] {
            new BigDecimal("0.45"),
            new BigDecimal("0.30"),
            new BigDecimal("0.10"),
            new BigDecimal("0.15")
          };
      case "D", "DF", "CB", "LB", "RB", "LWB", "RWB" ->
          new BigDecimal[] {
            new BigDecimal("0.10"),
            new BigDecimal("0.15"),
            new BigDecimal("0.55"),
            new BigDecimal("0.20")
          };
      case "G", "GK" ->
          new BigDecimal[] {
            new BigDecimal("0.05"),
            new BigDecimal("0.10"),
            new BigDecimal("0.50"),
            new BigDecimal("0.35")
          };
      default ->
          new BigDecimal[] {
            new BigDecimal("0.25"),
            new BigDecimal("0.30"),
            new BigDecimal("0.30"),
            new BigDecimal("0.15")
          };
    };
  }

  private BigDecimal weighted(
      BigDecimal first,
      BigDecimal firstWeight,
      BigDecimal second,
      BigDecimal secondWeight,
      BigDecimal third,
      BigDecimal thirdWeight) {
    return weighted(
        first, firstWeight, second, secondWeight, third, thirdWeight, null, BigDecimal.ZERO);
  }

  private BigDecimal weighted(
      BigDecimal first,
      BigDecimal firstWeight,
      BigDecimal second,
      BigDecimal secondWeight,
      BigDecimal third,
      BigDecimal thirdWeight,
      BigDecimal fourth,
      BigDecimal fourthWeight) {
    if (first == null && second == null && third == null && fourth == null) return null;
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal weightTotal = BigDecimal.ZERO;
    BigDecimal[] values = {first, second, third, fourth};
    BigDecimal[] weights = {firstWeight, secondWeight, thirdWeight, fourthWeight};
    for (int index = 0; index < values.length; index++) {
      if (values[index] != null && weights[index].signum() > 0) {
        total = total.add(values[index].multiply(weights[index]));
        weightTotal = weightTotal.add(weights[index]);
      }
    }
    return weightTotal.signum() == 0
        ? null
        : total.divide(weightTotal, SCALE + 2, RoundingMode.HALF_UP);
  }

  private BigDecimal normalize(BigDecimal value, String ceiling) {
    return value == null
        ? null
        : value
            .divide(new BigDecimal(ceiling), SCALE + 2, RoundingMode.HALF_UP)
            .max(BigDecimal.ZERO)
            .min(BigDecimal.ONE);
  }

  private long sum(
      List<MatchPlayerStatistic> observations,
      java.util.function.Function<MatchPlayerStatistic, Integer> extractor) {
    return observations.stream()
        .map(extractor)
        .filter(java.util.Objects::nonNull)
        .mapToLong(Integer::longValue)
        .sum();
  }

  private BigDecimal per90(long value, long minutes) {
    return BigDecimal.valueOf(value)
        .multiply(NINETY)
        .divide(BigDecimal.valueOf(minutes), SCALE + 2, RoundingMode.HALF_UP);
  }

  private BigDecimal per90Decimal(
      List<MatchPlayerStatistic> observations, long minutes, boolean expectedGoals) {
    BigDecimal total =
        observations.stream()
            .map(item -> expectedGoals ? item.getExpectedGoals() : item.getExpectedAssists())
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean available =
        observations.stream()
            .anyMatch(
                item ->
                    expectedGoals
                        ? item.getExpectedGoals() != null
                        : item.getExpectedAssists() != null);
    return available
        ? total
            .multiply(NINETY)
            .divide(BigDecimal.valueOf(minutes), SCALE + 2, RoundingMode.HALF_UP)
        : null;
  }

  private BigDecimal averageRating(List<MatchPlayerStatistic> observations) {
    List<BigDecimal> ratings =
        observations.stream()
            .map(MatchPlayerStatistic::getRating)
            .filter(java.util.Objects::nonNull)
            .toList();
    if (ratings.isEmpty()) return null;
    return ratings.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(ratings.size()), SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal scale(BigDecimal value) {
    return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
  }

  public record PlayerImpact(
      BigDecimal score,
      BigDecimal attackingIndex,
      BigDecimal creationIndex,
      BigDecimal defensiveIndex,
      BigDecimal averageRating,
      long minutesObserved,
      int matchesObserved) {}
}
