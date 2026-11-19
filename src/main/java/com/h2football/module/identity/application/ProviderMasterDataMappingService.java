package com.h2football.module.identity.application;

import com.h2football.module.identity.domain.model.ExternalIdentity;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.application.MasterDataNotFoundException;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProviderMasterDataMappingService {
  private static final List<ResourceType> SUPPORTED_RESOURCE_TYPES =
      List.of(ResourceType.LEAGUE, ResourceType.SEASON, ResourceType.TEAM, ResourceType.PLAYER);

  private final ExternalIdentityService identities;
  private final LeagueRepository leagues;
  private final SeasonRepository seasons;
  private final TeamRepository teams;
  private final PlayerRepository players;

  public ProviderMasterDataMappingService(
      ExternalIdentityService identities,
      LeagueRepository leagues,
      SeasonRepository seasons,
      TeamRepository teams,
      PlayerRepository players) {
    this.identities = identities;
    this.leagues = leagues;
    this.seasons = seasons;
    this.teams = teams;
    this.players = players;
  }

  public MappingResult map(
      Provider provider, ResourceType resourceType, String externalId, Long canonicalId) {
    validateResource(resourceType, canonicalId);
    ExternalIdentity identity =
        identities.register(resourceType, provider, externalId, canonicalId);
    return toResult(identity);
  }

  public List<MappingResult> mapAll(List<MappingCommand> commands) {
    if (commands == null || commands.isEmpty()) {
      throw new IllegalArgumentException("At least one provider mapping is required");
    }
    return commands.stream()
        .map(
            command ->
                map(
                    command.provider(),
                    command.resourceType(),
                    command.externalId(),
                    command.canonicalId()))
        .toList();
  }

  private void validateResource(ResourceType resourceType, Long canonicalId) {
    if (resourceType == null || !SUPPORTED_RESOURCE_TYPES.contains(resourceType)) {
      throw new IllegalArgumentException(
          "Only LEAGUE, SEASON, TEAM and PLAYER mappings are supported");
    }
    if (canonicalId == null || canonicalId <= 0) {
      throw new IllegalArgumentException("canonicalId must be positive");
    }
    boolean exists =
        switch (resourceType) {
          case LEAGUE -> leagues.existsById(canonicalId);
          case SEASON -> seasons.existsById(canonicalId);
          case TEAM -> teams.existsById(canonicalId);
          case PLAYER -> players.existsById(canonicalId);
          default -> false;
        };
    if (!exists) {
      throw new MasterDataNotFoundException(resourceType.name(), canonicalId);
    }
  }

  private MappingResult toResult(ExternalIdentity identity) {
    return new MappingResult(
        identity.getId(),
        identity.getResourceType(),
        identity.getProvider(),
        identity.getExternalId(),
        identity.getCanonicalId(),
        identity.getCreatedAt());
  }

  public record MappingCommand(
      Provider provider, ResourceType resourceType, String externalId, Long canonicalId) {}

  public record MappingResult(
      Long id,
      ResourceType resourceType,
      Provider provider,
      String externalId,
      Long canonicalId,
      java.time.Instant createdAt) {}
}
