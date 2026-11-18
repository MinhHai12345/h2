package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic.PlayerSeasonStatisticSnapshot;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerPerformanceAnalyticsApplicationServiceTest {
  private PlayerSeasonStatisticsApplicationService seasonStatistics;
  private PlayerPerformanceAnalyticsApplicationService service;

  @BeforeEach
  void setUp() {
    seasonStatistics = mock(PlayerSeasonStatisticsApplicationService.class);
    service =
        new PlayerPerformanceAnalyticsApplicationService(
            seasonStatistics, new PlayerPerformanceCalculator());
  }

  @Test
  void filtersByMinutesAndPositionAndSortsByGoalContributionsPerNinety() {
    var forward = statistic("Forward", "F", 900L, 10L, 5L);
    var midfielder = statistic("Midfielder", "M", 900L, 4L, 8L);
    var lowMinutes = statistic("Low minutes", "F", 100L, 5L, 0L);
    when(seasonStatistics.findBySeason(2025L, Provider.SOFASCORE))
        .thenReturn(List.of(midfielder, lowMinutes, forward));

    var result = service.findBySeason(2025L, Provider.SOFASCORE, 450L, "F");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).playerName()).isEqualTo("Forward");
    assertThat(result.get(0).goalContributionsPer90()).isEqualByComparingTo("1.500");
  }

  private PlayerSeasonStatistic statistic(
      String name, String position, long minutes, long goals, long assists) {
    return new PlayerSeasonStatistic(
        mock(Season.class),
        new Player(name, null, null, position, null, null),
        Provider.SOFASCORE,
        new PlayerSeasonStatisticSnapshot(
            10L, minutes, goals, assists, 20L, 10L, 100L, 120L, 5L, 3L, 2L, 1L, null, null, null));
  }
}
