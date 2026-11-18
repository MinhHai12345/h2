package com.h2football.module.ingestion.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.match.application.MatchIncidentApplicationService;
import com.h2football.module.match.application.MatchIncidentApplicationService.IncidentInput;
import com.h2football.module.match.domain.model.IncidentSide;
import com.h2football.module.player.application.PlayerApplicationService;
import com.h2football.module.player.application.PlayerApplicationService.PlayerInput;
import com.h2football.module.provider.sofascore.SofaScoreMatchEventProvider;
import com.h2football.module.provider.sofascore.model.SofaScoreIncidentSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScorePlayerSnapshot;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SofaScoreMatchIncidentIngestionService {
  private static final Provider PROVIDER = Provider.SOFASCORE;
  private final SofaScoreMatchEventProvider provider;
  private final ExternalIdentityService identities;
  private final PlayerApplicationService players;
  private final MatchIncidentApplicationService incidents;

  public SofaScoreMatchIncidentIngestionService(
      SofaScoreMatchEventProvider provider,
      ExternalIdentityService identities,
      PlayerApplicationService players,
      MatchIncidentApplicationService incidents) {
    this.provider = provider;
    this.identities = identities;
    this.players = players;
    this.incidents = incidents;
  }

  public IncidentSyncResult sync(long sofaScoreEventId) {
    Long matchId = findCanonicalMatchId(sofaScoreEventId);
    List<SofaScoreIncidentSnapshot> snapshots = provider.fetchIncidents(sofaScoreEventId);
    List<IncidentInput> inputs =
        snapshots.stream()
            .map(
                item ->
                    new IncidentInput(
                        item.externalId(),
                        item.sortOrder(),
                        item.incidentType(),
                        item.incidentClass(),
                        item.minute(),
                        item.addedTime(),
                        side(item.isHome()),
                        item.text(),
                        item.reason(),
                        item.homeScore(),
                        item.awayScore(),
                        item.rescinded(),
                        upsertPlayer(item.player()),
                        upsertPlayer(item.assistPlayer()),
                        upsertPlayer(item.playerIn()),
                        upsertPlayer(item.playerOut())))
            .toList();
    int stored = incidents.replaceFromProvider(matchId, PROVIDER, inputs);
    return new IncidentSyncResult(sofaScoreEventId, matchId, snapshots.size(), stored);
  }

  private Long upsertPlayer(SofaScorePlayerSnapshot player) {
    if (player == null) return null;
    return players.upsertFromProvider(
        PROVIDER,
        player.externalId(),
        new PlayerInput(
            player.name(),
            player.shortName(),
            player.slug(),
            player.position(),
            player.countryCode(),
            player.countryName()));
  }

  private IncidentSide side(Boolean isHome) {
    return isHome == null ? IncidentSide.NEUTRAL : isHome ? IncidentSide.HOME : IncidentSide.AWAY;
  }

  private Long findCanonicalMatchId(long eventId) {
    if (eventId <= 0) throw new IllegalArgumentException("sofaScoreEventId must be positive");
    return identities
        .findCanonicalId(ResourceType.MATCH, PROVIDER, Long.toString(eventId))
        .orElseThrow(
            () -> new IllegalStateException("SofaScore event is not mapped; sync matches first"));
  }

  public record IncidentSyncResult(
      long sofaScoreEventId, long canonicalMatchId, int discoveredIncidents, int storedIncidents) {}
}
