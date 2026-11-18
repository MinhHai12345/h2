package com.h2football.module.player.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Season;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PlayerSeasonStatisticTest {
  @Test
  void storesSeasonAggregateSnapshot() {
    var statistic =
        new PlayerSeasonStatistic(
            mock(Season.class),
            mock(Player.class),
            Provider.SOFASCORE,
            new PlayerSeasonStatistic.PlayerSeasonStatisticSnapshot(
                10L,
                900L,
                7L,
                3L,
                28L,
                12L,
                220L,
                260L,
                18L,
                8L,
                4L,
                2L,
                new BigDecimal("5.25"),
                new BigDecimal("2.75"),
                new BigDecimal("7.450")));

    assertThat(statistic.getAppearances()).isEqualTo(10L);
    assertThat(statistic.getGoals()).isEqualTo(7L);
    assertThat(statistic.getExpectedGoals()).isEqualByComparingTo("5.25");
    assertThat(statistic.getAverageRating()).isEqualByComparingTo("7.450");
  }

  @Test
  void rejectsNegativeAggregates() {
    assertThatThrownBy(
            () ->
                new PlayerSeasonStatistic(
                    mock(Season.class),
                    mock(Player.class),
                    Provider.SOFASCORE,
                    new PlayerSeasonStatistic.PlayerSeasonStatisticSnapshot(
                        1L, -1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
