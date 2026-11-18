package com.h2football.module.match.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import com.h2football.module.match.domain.repository.MatchPlayerStatisticRepository;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchPlayerStatisticsApplicationServiceTest {
  private final MatchRepository matches = mock(MatchRepository.class);
  private final PlayerRepository players = mock(PlayerRepository.class);
  private final MatchPlayerStatisticRepository statistics =
      mock(MatchPlayerStatisticRepository.class);
  private final MatchPlayerStatisticsApplicationService service =
      new MatchPlayerStatisticsApplicationService(matches, players, statistics);

  @Test
  void upsertsProviderSnapshotWithNormalizedAndRawValues() {
    Match match = mock(Match.class);
    Player player = mock(Player.class);
    when(matches.existsById(10L)).thenReturn(true);
    when(matches.findById(10L)).thenReturn(Optional.of(match));
    when(players.findById(101L)).thenReturn(Optional.of(player));
    when(statistics.findByMatchIdAndPlayerIdAndProvider(10L, 101L, Provider.SOFASCORE))
        .thenReturn(Optional.empty());

    int stored =
        service.upsertFromProvider(
            10L,
            Provider.SOFASCORE,
            List.of(
                new MatchPlayerStatisticsApplicationService.PlayerStatisticInput(
                    101L,
                    new BigDecimal("7.8"),
                    90,
                    1,
                    0,
                    4,
                    2,
                    31,
                    36,
                    2,
                    1,
                    0,
                    0,
                    new BigDecimal("0.72"),
                    new BigDecimal("0.18"),
                    "{\"rating\":7.8}")));

    assertThat(stored).isEqualTo(1);
    verify(statistics).save(any(MatchPlayerStatistic.class));
  }

  @Test
  void preservesExistingSnapshotWhenSourceReturnsNoPlayerStatistics() {
    when(matches.existsById(10L)).thenReturn(true);

    assertThat(service.upsertFromProvider(10L, Provider.SOFASCORE, List.of())).isZero();

    verify(statistics, never()).save(any());
  }
}
