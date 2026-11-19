package com.h2football.module.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class MasterDataReconciliationServiceTest {
  private final LeagueRepository leagues = mock(LeagueRepository.class);
  private final SeasonRepository seasons = mock(SeasonRepository.class);
  private final TeamRepository teams = mock(TeamRepository.class);
  private final PlayerRepository players = mock(PlayerRepository.class);
  private final MasterDataReconciliationService service =
      new MasterDataReconciliationService(leagues, seasons, teams, players);

  @Test
  void reportsDuplicateTeamCandidatesWithoutMergingThem() {
    Country country = new Country("GB", "England");
    Team first = mock(Team.class);
    Team second = mock(Team.class);
    when(first.getId()).thenReturn(10L);
    when(second.getId()).thenReturn(11L);
    when(first.getName()).thenReturn("Manchester United");
    when(second.getName()).thenReturn("Manchester United FC");
    when(first.getCountry()).thenReturn(country);
    when(second.getCountry()).thenReturn(country);
    when(teams.findAll()).thenReturn(List.of(first, second));

    var candidates = service.findCandidates(ResourceType.TEAM);

    assertThat(candidates).hasSize(1);
    assertThat(candidates.get(0).canonicalIds()).containsExactly(10L, 11L);
    assertThat(candidates.get(0).status()).isEqualTo("REVIEW_REQUIRED");
    assertThat(candidates.get(0).entities()).hasSize(2);
    verify(teams, never()).delete(any());
  }

  @Test
  void reportsPlayersOnlyWhenCountryCodeIsAvailable() {
    Player first = mock(Player.class);
    Player second = mock(Player.class);
    Player withoutCountry = mock(Player.class);
    when(first.getId()).thenReturn(21L);
    when(second.getId()).thenReturn(22L);
    when(first.getName()).thenReturn("João Silva");
    when(second.getName()).thenReturn("Joao Silva");
    when(first.getCountryCode()).thenReturn("BR");
    when(second.getCountryCode()).thenReturn("br");
    when(withoutCountry.getName()).thenReturn("Joao Silva");
    when(withoutCountry.getCountryCode()).thenReturn(null);
    when(players.findAll()).thenReturn(List.of(first, second, withoutCountry));

    var candidates = service.findCandidates(ResourceType.PLAYER);

    assertThat(candidates).hasSize(1);
    assertThat(candidates.get(0).canonicalIds()).containsExactly(21L, 22L);
    assertThat(candidates.get(0).reason()).contains("verify player identity");
  }
}
