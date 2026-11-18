package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2football.module.analytics.domain.model.TeamSeasonStatistic;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeamPerformanceCalculatorTest {
  private final TeamPerformanceCalculator calculator = new TeamPerformanceCalculator();

  @Test
  void calculatesExpectedGoalsDifferenceAndShotAccuracy() {
    TeamSeasonStatistic expectedGoals = statistic("expectedGoals", "1.65");
    TeamSeasonStatistic shots = statistic("totalShots", "14");
    TeamSeasonStatistic shotsOnTarget = statistic("shotsOnTarget", "5.6");
    TeamSeasonStatistic possession = statistic("ballPossession", "57.4");

    var result =
        calculator.calculate(
            List.of(expectedGoals, shots, shotsOnTarget, possession), new BigDecimal("1.10"));

    assertThat(result.expectedGoalsFor()).isEqualByComparingTo("1.65");
    assertThat(result.expectedGoalsAgainst()).isEqualByComparingTo("1.10");
    assertThat(result.expectedGoalsDifference()).isEqualByComparingTo("0.550");
    assertThat(result.shotAccuracyPercent()).isEqualByComparingTo("40.000");
    assertThat(result.possessionPercent()).isEqualByComparingTo("57.4");
  }

  @Test
  void leavesUnavailableMetricsNullInsteadOfInventingValues() {
    var result = calculator.calculate(List.of(), null);

    assertThat(result.expectedGoalsFor()).isNull();
    assertThat(result.expectedGoalsAgainst()).isNull();
    assertThat(result.expectedGoalsDifference()).isNull();
    assertThat(result.shotAccuracyPercent()).isNull();
  }

  @Test
  void rejectsNullSource() {
    assertThatThrownBy(() -> calculator.calculate(null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private TeamSeasonStatistic statistic(String key, String average) {
    TeamSeasonStatistic item = mock(TeamSeasonStatistic.class);
    when(item.getPeriod()).thenReturn("ALL");
    when(item.getStatisticKey()).thenReturn(key);
    when(item.getAverageValue()).thenReturn(new BigDecimal(average));
    return item;
  }
}
