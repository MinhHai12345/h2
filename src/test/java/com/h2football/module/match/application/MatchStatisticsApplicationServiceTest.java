package com.h2football.module.match.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchStatisticsApplicationServiceTest {
  private final MatchRepository matches = mock(MatchRepository.class);
  private final MatchStatisticRepository statistics = mock(MatchStatisticRepository.class);
  private final MatchStatisticsApplicationService service =
      new MatchStatisticsApplicationService(matches, statistics);

  @Test
  void replacesProviderSnapshotAndPersistsNumericValues() {
    Match match = mock(Match.class);
    when(matches.existsById(12L)).thenReturn(true);
    when(matches.findById(12L)).thenReturn(Optional.of(match));
    int count =
        service.replaceFromProvider(
            12L,
            Provider.SOFASCORE,
            List.of(
                new MatchStatisticsApplicationService.StatisticInput(
                    "ALL",
                    "Match overview",
                    "expectedGoals",
                    "Expected goals",
                    "1.86",
                    "0.71",
                    new BigDecimal("1.86"),
                    new BigDecimal("0.71"))));
    assertThat(count).isEqualTo(1);
    verify(statistics).deleteAllByMatchIdAndProvider(12L, Provider.SOFASCORE);
    verify(statistics).saveAll(anyList());
  }

  @Test
  void keepsStoredSnapshotWhenProviderHasNoStatistics() {
    when(matches.existsById(12L)).thenReturn(true);
    assertThat(service.replaceFromProvider(12L, Provider.SOFASCORE, List.of())).isZero();
    verify(statistics, never()).deleteAllByMatchIdAndProvider(anyLong(), any());
    verify(statistics, never()).saveAll(anyList());
  }
}
