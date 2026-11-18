package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.match.domain.repository.MatchPlayerStatisticRepository;
import com.h2football.module.match.domain.repository.SeasonPlayerStatisticProjection;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
import com.h2football.module.player.domain.repository.PlayerRepository;
import com.h2football.module.player.domain.repository.PlayerSeasonStatisticRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlayerSeasonStatisticsApplicationServiceTest {
  private final SeasonRepository seasons = mock(SeasonRepository.class);
  private final PlayerRepository players = mock(PlayerRepository.class);
  private final MatchPlayerStatisticRepository matchStatistics =
      mock(MatchPlayerStatisticRepository.class);
  private final PlayerSeasonStatisticRepository seasonStatistics =
      mock(PlayerSeasonStatisticRepository.class);
  private final PlayerSeasonStatisticsApplicationService service =
      new PlayerSeasonStatisticsApplicationService(
          seasons, players, matchStatistics, seasonStatistics);

  @Test
  void rebuildsSeasonAggregatesFromMatchStatistics() {
    Season season = mock(Season.class);
    Player player = mock(Player.class);
    SeasonPlayerStatisticProjection projection = mock(SeasonPlayerStatisticProjection.class);
    when(seasons.findById(20L)).thenReturn(Optional.of(season));
    when(matchStatistics.aggregateBySeasonAndProvider(20L, Provider.SOFASCORE))
        .thenReturn(List.of(projection));
    when(projection.getPlayerId()).thenReturn(101L);
    when(projection.getAppearances()).thenReturn(12L);
    when(projection.getMinutesPlayed()).thenReturn(980L);
    when(projection.getGoals()).thenReturn(8L);
    when(projection.getAssists()).thenReturn(4L);
    when(projection.getTotalShots()).thenReturn(35L);
    when(projection.getShotsOnTarget()).thenReturn(18L);
    when(projection.getAccuratePasses()).thenReturn(240L);
    when(projection.getTotalPasses()).thenReturn(300L);
    when(projection.getKeyPasses()).thenReturn(21L);
    when(projection.getTackles()).thenReturn(9L);
    when(projection.getInterceptions()).thenReturn(5L);
    when(projection.getClearances()).thenReturn(3L);
    when(projection.getExpectedGoals()).thenReturn(new BigDecimal("6.2500"));
    when(projection.getExpectedAssists()).thenReturn(new BigDecimal("3.1000"));
    when(projection.getAverageRating()).thenReturn(7.625);
    when(players.findById(101L)).thenReturn(Optional.of(player));
    when(seasonStatistics.findBySeasonIdAndPlayerIdAndProvider(20L, 101L, Provider.SOFASCORE))
        .thenReturn(Optional.empty());

    var result = service.rebuild(20L, Provider.SOFASCORE);

    assertThat(result.aggregatedPlayers()).isEqualTo(1);
    assertThat(result.storedPlayers()).isEqualTo(1);
    verify(seasonStatistics).save(any(PlayerSeasonStatistic.class));
  }

  @Test
  void keepsExistingAggregatesWhenThereAreNoSourceObservations() {
    when(seasons.findById(20L)).thenReturn(Optional.of(mock(Season.class)));
    when(matchStatistics.aggregateBySeasonAndProvider(20L, Provider.SOFASCORE))
        .thenReturn(List.of());

    var result = service.rebuild(20L, Provider.SOFASCORE);

    assertThat(result.storedPlayers()).isZero();
    verify(seasonStatistics, never()).save(any());
  }

  @Test
  void rejectsInvalidSeasonId() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.rebuild(0L, Provider.SOFASCORE))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
