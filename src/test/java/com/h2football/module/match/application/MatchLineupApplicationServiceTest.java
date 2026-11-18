package com.h2football.module.match.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.repository.MatchLineupRepository;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchLineupApplicationServiceTest {
  private final MatchRepository matches = mock(MatchRepository.class);
  private final TeamRepository teams = mock(TeamRepository.class);
  private final PlayerRepository players = mock(PlayerRepository.class);
  private final MatchLineupRepository lineups = mock(MatchLineupRepository.class);
  private final MatchLineupApplicationService service =
      new MatchLineupApplicationService(matches, teams, players, lineups);

  @Test
  void doesNotDeleteExistingLineupWhenProviderReturnsNoPlayers() {
    when(matches.existsById(10L)).thenReturn(true);

    int stored = service.replaceFromProvider(10L, Provider.SOFASCORE, List.of());

    assertThat(stored).isZero();
    verify(lineups, never()).deleteAllByMatchIdAndTeamIdAndProvider(anyLong(), anyLong(), any());
    verify(lineups, never()).saveAll(anyList());
  }
}
