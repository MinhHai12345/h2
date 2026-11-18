package com.h2football.module.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.match.application.MatchPlayerStatisticsApplicationService;
import com.h2football.module.player.application.PlayerApplicationService;
import com.h2football.module.provider.sofascore.SofaScoreMatchEventProvider;
import com.h2football.module.provider.sofascore.model.SofaScoreLineupSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScorePlayerSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScorePlayerStatisticsSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SofaScorePlayerMatchStatisticsIngestionServiceTest {
  private final SofaScoreMatchEventProvider provider = mock(SofaScoreMatchEventProvider.class);
  private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
  private final PlayerApplicationService players = mock(PlayerApplicationService.class);
  private final MatchPlayerStatisticsApplicationService statistics =
      mock(MatchPlayerStatisticsApplicationService.class);
  private final SofaScorePlayerMatchStatisticsIngestionService service =
      new SofaScorePlayerMatchStatisticsIngestionService(provider, identities, players, statistics);

  @Test
  void mapsLineupPlayerStatisticsIntoCanonicalMatchSnapshot() {
    when(identities.findCanonicalId(ResourceType.MATCH, Provider.SOFASCORE, "900"))
        .thenReturn(Optional.of(12L));
    when(players.upsertFromProvider(eq(Provider.SOFASCORE), eq("101"), any())).thenReturn(101L);
    var player =
        new SofaScorePlayerSnapshot("101", "Player One", "P. One", "player-one", "F", null, null);
    var stats =
        new SofaScorePlayerStatisticsSnapshot(
            "{\"rating\":7.6,\"goals\":1}",
            Map.of("rating", "7.6", "goals", "1", "minutesPlayed", "90"));
    var lineupPlayer =
        new SofaScoreLineupSnapshot.LineupPlayer(player, "F", "9", false, false, null, stats);
    when(provider.fetchLineups(900))
        .thenReturn(
            new SofaScoreLineupSnapshot(
                true,
                new SofaScoreLineupSnapshot.TeamLineup("4-3-3", List.of(lineupPlayer)),
                new SofaScoreLineupSnapshot.TeamLineup("4-4-2", List.of())));

    var result = service.sync(900);

    assertThat(result.discoveredPlayers()).isEqualTo(1);
    verify(statistics).upsertFromProvider(eq(12L), eq(Provider.SOFASCORE), anyList());
  }

  @Test
  void doesNotEraseExistingStatisticsWhenLineupHasNoStatistics() {
    when(identities.findCanonicalId(ResourceType.MATCH, Provider.SOFASCORE, "900"))
        .thenReturn(Optional.of(12L));
    when(provider.fetchLineups(900))
        .thenReturn(
            new SofaScoreLineupSnapshot(
                false,
                new SofaScoreLineupSnapshot.TeamLineup("4-3-3", List.of()),
                new SofaScoreLineupSnapshot.TeamLineup("4-4-2", List.of())));

    assertThat(service.sync(900).storedPlayers()).isZero();
    verify(statistics, never()).upsertFromProvider(anyLong(), any(), anyList());
  }
}
