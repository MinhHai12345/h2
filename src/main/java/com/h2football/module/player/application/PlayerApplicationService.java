package com.h2football.module.player.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlayerApplicationService {
  private final PlayerRepository players;
  private final ExternalIdentityService identities;

  public PlayerApplicationService(PlayerRepository players, ExternalIdentityService identities) {
    this.players = players;
    this.identities = identities;
  }

  public Long upsertFromProvider(Provider provider, String externalId, PlayerInput input) {
    if (input == null) throw new IllegalArgumentException("Player input is required");
    Long canonicalId =
        identities.findCanonicalId(ResourceType.PLAYER, provider, externalId).orElse(null);
    if (canonicalId != null) {
      Player existing =
          players
              .findById(canonicalId)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Player identity references missing canonical ID " + canonicalId));
      existing.updateProfile(
          input.name(),
          input.shortName(),
          input.slug(),
          input.position(),
          input.countryCode(),
          input.countryName());
      return existing.getId();
    }
    Player canonical =
        players.save(
            new Player(
                input.name(),
                input.shortName(),
                input.slug(),
                input.position(),
                input.countryCode(),
                input.countryName()));
    identities.register(ResourceType.PLAYER, provider, externalId, canonical.getId());
    return canonical.getId();
  }

  private String normalizeCountryCode(String value) {
    return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
  }

  public record PlayerInput(
      String name,
      String shortName,
      String slug,
      String position,
      String countryCode,
      String countryName) {}
}
