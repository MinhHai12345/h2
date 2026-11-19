package com.h2football.module.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.ExternalIdentity;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.player.domain.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

class ProviderMasterDataMappingServiceTest {
  private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
  private final LeagueRepository leagues = mock(LeagueRepository.class);
  private final SeasonRepository seasons = mock(SeasonRepository.class);
  private final TeamRepository teams = mock(TeamRepository.class);
  private final PlayerRepository players = mock(PlayerRepository.class);
  private final ProviderMasterDataMappingService service =
      new ProviderMasterDataMappingService(identities, leagues, seasons, teams, players);

  @Test
  void mapsProviderTeamIdentityToExistingCanonicalTeam() {
    when(teams.existsById(200L)).thenReturn(true);
    when(identities.register(ResourceType.TEAM, Provider.SOFASCORE, "team-42", 200L))
        .thenReturn(new ExternalIdentity(ResourceType.TEAM, Provider.SOFASCORE, "team-42", 200L));

    var result = service.map(Provider.SOFASCORE, ResourceType.TEAM, "team-42", 200L);

    assertThat(result.resourceType()).isEqualTo(ResourceType.TEAM);
    assertThat(result.provider()).isEqualTo(Provider.SOFASCORE);
    assertThat(result.externalId()).isEqualTo("team-42");
    assertThat(result.canonicalId()).isEqualTo(200L);
  }

  @Test
  void rejectsMappingWhenCanonicalEntityDoesNotExist() {
    when(players.existsById(999L)).thenReturn(false);

    assertThatThrownBy(
            () -> service.map(Provider.UNDERSTAT, ResourceType.PLAYER, "player-9", 999L))
        .isInstanceOf(
            com.h2football.module.masterdata.application.MasterDataNotFoundException.class);

    verify(identities, never()).register(any(), any(), anyString(), anyLong());
  }

  @Test
  void rejectsUnsupportedResourceTypes() {
    assertThatThrownBy(
            () -> service.map(Provider.SOFASCORE, ResourceType.MATCH, "match-1", 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Only LEAGUE");
  }
}
