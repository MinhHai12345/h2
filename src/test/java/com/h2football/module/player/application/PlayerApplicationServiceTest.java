package com.h2football.module.player.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlayerApplicationServiceTest {
  private final PlayerRepository players = mock(PlayerRepository.class);
  private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
  private final PlayerApplicationService service =
      new PlayerApplicationService(players, identities);

  @Test
  void createsCanonicalPlayerAndRegistersProviderIdentity() {
    when(identities.findCanonicalId(any(), eq(Provider.SOFASCORE), eq("101")))
        .thenReturn(Optional.empty());
    Player saved = mock(Player.class);
    when(saved.getId()).thenReturn(55L);
    when(players.save(any(Player.class))).thenReturn(saved);

    Long id =
        service.upsertFromProvider(
            Provider.SOFASCORE,
            "101",
            new PlayerApplicationService.PlayerInput(
                "Player One", "P. One", "player-one", "F", "BR", "Brazil"));

    assertThat(id).isEqualTo(55L);
    verify(identities).register(any(), eq(Provider.SOFASCORE), eq("101"), eq(55L));
  }

  @Test
  void updatesExistingCanonicalPlayerWithoutCreatingAnotherIdentity() {
    Player existing = mock(Player.class);
    when(identities.findCanonicalId(any(), eq(Provider.SOFASCORE), eq("101")))
        .thenReturn(Optional.of(55L));
    when(players.findById(55L)).thenReturn(Optional.of(existing));
    when(existing.getId()).thenReturn(55L);

    Long id =
        service.upsertFromProvider(
            Provider.SOFASCORE,
            "101",
            new PlayerApplicationService.PlayerInput("Updated Name", null, null, "M", null, null));

    assertThat(id).isEqualTo(55L);
    verify(existing).updateProfile("Updated Name", null, null, "M", null, null);
    verify(players, never()).save(any());
    verify(identities, never()).register(any(), any(), any(), any());
  }

  @Test
  void linksUniqueExistingPlayerByNormalizedNameAndCountry() {
    Player existing = mock(Player.class);
    when(existing.getId()).thenReturn(88L);
    when(existing.getName()).thenReturn("João Silva");
    when(existing.getCountryCode()).thenReturn("BR");
    when(identities.findCanonicalId(any(), eq(Provider.UNDERSTAT), eq("player-88")))
        .thenReturn(Optional.empty());
    when(players.findAll()).thenReturn(java.util.List.of(existing));

    Long id =
        service.upsertFromProvider(
            Provider.UNDERSTAT,
            "player-88",
            new PlayerApplicationService.PlayerInput(
                "Joao Silva", null, null, null, "br", "Brazil"));

    assertThat(id).isEqualTo(88L);
    verify(identities).register(any(), eq(Provider.UNDERSTAT), eq("player-88"), eq(88L));
    verify(players, never()).save(any());
  }

  @Test
  void rejectsAmbiguousPlayerIdentityInsteadOfCreatingDuplicate() {
    Player first = mock(Player.class);
    Player second = mock(Player.class);
    when(first.getName()).thenReturn("Alex Smith");
    when(second.getName()).thenReturn("Alex Smith");
    when(first.getCountryCode()).thenReturn("GB");
    when(second.getCountryCode()).thenReturn("GB");
    when(identities.findCanonicalId(any(), eq(Provider.UNDERSTAT), eq("player-x")))
        .thenReturn(Optional.empty());
    when(players.findAll()).thenReturn(java.util.List.of(first, second));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.upsertFromProvider(
                    Provider.UNDERSTAT,
                    "player-x",
                    new PlayerApplicationService.PlayerInput(
                        "Alex Smith", null, null, null, "GB", "England")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Ambiguous canonical player identity");
    verify(players, never()).save(any());
  }

  @Test
  void preservesOptionalProfileFieldsWhenProviderReturnsPartialPlayer() {
    Player player = new Player("Player One", "P. One", "player-one", "F", "BR", "Brazil");

    player.updateProfile("Player One Updated", null, null, null, null, null);

    assertThat(player.getName()).isEqualTo("Player One Updated");
    assertThat(player.getShortName()).isEqualTo("P. One");
    assertThat(player.getSlug()).isEqualTo("player-one");
    assertThat(player.getPosition()).isEqualTo("F");
    assertThat(player.getCountryCode()).isEqualTo("BR");
    assertThat(player.getCountryName()).isEqualTo("Brazil");
  }
}
