package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic.PlayerSeasonStatisticSnapshot;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerPerformanceCalculatorTest {
  private PlayerPerformanceCalculator calculator;
  private PlayerSeasonStatistic statistic;

  @BeforeEach
  void setUp() {
    calculator = new PlayerPerformanceCalculator();
    statistic =
        new PlayerSeasonStatistic(
            org.mockito.Mockito.mock(Season.class),
            new Player("Test Player", null, null, "F", null, null),
            Provider.SOFASCORE,
            new PlayerSeasonStatisticSnapshot(
                10L,
                900L,
                12L,
                6L,
                40L,
                20L,
                300L,
                360L,
                18L,
                10L,
                8L,
                5L,
                new BigDecimal("9.50"),
                new BigDecimal("4.50"),
                new BigDecimal("7.250")));
  }

  @Test
  void calculatesPerNinetyRatesAndPercentages() {
    var result = calculator.calculate(statistic);
    assertThat(result.goalsPer90()).isEqualByComparingTo("1.200");
    assertThat(result.assistsPer90()).isEqualByComparingTo("0.600");
    assertThat(result.goalContributionsPer90()).isEqualByComparingTo("1.800");
    assertThat(result.expectedGoalsPer90()).isEqualByComparingTo("0.950");
    assertThat(result.defensiveActionsPer90()).isEqualByComparingTo("2.300");
    assertThat(result.shotAccuracyPercent()).isEqualByComparingTo("50.000");
    assertThat(result.passAccuracyPercent()).isEqualByComparingTo("83.333");
    assertThat(result.goalConversionPercent()).isEqualByComparingTo("30.000");
    assertThat(result.goalsMinusExpectedGoals()).isEqualByComparingTo("2.500");
  }

  @Test
  void returnsNullRatesWhenMinutesOrDenominatorsAreZero() {
    var withoutMinutes =
        new PlayerSeasonStatistic(
            org.mockito.Mockito.mock(Season.class),
            new Player("No Minutes", null, null, "M", null, null),
            Provider.SOFASCORE,
            new PlayerSeasonStatisticSnapshot(
                1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, null, null, null));
    var result = calculator.calculate(withoutMinutes);
    assertThat(result.goalsPer90()).isNull();
    assertThat(result.expectedGoalsPer90()).isNull();
    assertThat(result.shotAccuracyPercent()).isNull();
    assertThat(result.passAccuracyPercent()).isNull();
    assertThat(result.goalConversionPercent()).isNull();
    assertThat(result.goalsMinusExpectedGoals()).isNull();
  }

  @Test
  void rejectsMissingSeasonStatistics() {
    assertThatThrownBy(() -> calculator.calculate(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
