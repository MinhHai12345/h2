package com.h2football.module.match.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.repository.MatchIncidentRepository;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchIncidentApplicationServiceTest {
  private final MatchRepository matches = mock(MatchRepository.class);
  private final PlayerRepository players = mock(PlayerRepository.class);
  private final MatchIncidentRepository incidents = mock(MatchIncidentRepository.class);
  private final MatchIncidentApplicationService service =
      new MatchIncidentApplicationService(matches, players, incidents);

  @Test
  void doesNotDeleteExistingTimelineWhenProviderReturnsNoIncidents() {
    when(matches.existsById(10L)).thenReturn(true);

    int stored = service.replaceFromProvider(10L, Provider.SOFASCORE, List.of());

    assertThat(stored).isZero();
    verify(incidents, never()).deleteAllByMatchIdAndProvider(anyLong(), any());
    verify(incidents, never()).saveAll(anyList());
  }
}
