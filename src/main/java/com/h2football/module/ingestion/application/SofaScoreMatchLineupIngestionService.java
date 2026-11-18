package com.h2football.module.ingestion.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.match.application.MatchApplicationService;
import com.h2football.module.match.application.MatchLineupApplicationService;
import com.h2football.module.match.application.MatchLineupApplicationService.LineupPlayerInput;
import com.h2football.module.match.application.MatchLineupApplicationService.TeamLineupInput;
import com.h2football.module.match.domain.model.LineupRole;
import com.h2football.module.player.application.PlayerApplicationService;
import com.h2football.module.player.application.PlayerApplicationService.PlayerInput;
import com.h2football.module.provider.sofascore.SofaScoreMatchEventProvider;
import com.h2football.module.provider.sofascore.model.SofaScoreLineupSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScorePlayerSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SofaScoreMatchLineupIngestionService {
  private static final Provider PROVIDER = Provider.SOFASCORE;
  private final SofaScoreMatchEventProvider provider;
  private final ExternalIdentityService identities;
  private final MatchApplicationService matches;
  private final PlayerApplicationService players;
  private final MatchLineupApplicationService lineups;

  public SofaScoreMatchLineupIngestionService(
      SofaScoreMatchEventProvider provider,
      ExternalIdentityService identities,
      MatchApplicationService matches,
      PlayerApplicationService players,
      MatchLineupApplicationService lineups) {
    this.provider = provider;
    this.identities = identities;
    this.matches = matches;
    this.players = players;
    this.lineups = lineups;
  }

  public LineupSyncResult sync(long sofaScoreEventId) {
    Long matchId = findCanonicalMatchId(sofaScoreEventId);
    var match = matches.get(matchId);
    SofaScoreLineupSnapshot snapshot = provider.fetchLineups(sofaScoreEventId);
    if (snapshot == null || (empty(snapshot.home()) && empty(snapshot.away()))) {
      return new LineupSyncResult(sofaScoreEventId, matchId, false, 0);
    }
    List<TeamLineupInput> inputs = new ArrayList<>();
    if (!empty(snapshot.home())) {
      inputs.add(toInput(match.getHomeTeam().getId(), snapshot.home(), snapshot.confirmed()));
    }
    if (!empty(snapshot.away())) {
      inputs.add(toInput(match.getAwayTeam().getId(), snapshot.away(), snapshot.confirmed()));
    }
    int stored = lineups.replaceFromProvider(matchId, PROVIDER, inputs);
    return new LineupSyncResult(sofaScoreEventId, matchId, snapshot.confirmed(), stored);
  }

  private TeamLineupInput toInput(
      Long teamId, SofaScoreLineupSnapshot.TeamLineup source, boolean confirmed) {
    List<LineupPlayerInput> playerInputs =
        source.players().stream()
            .map(
                item -> {
                  Long playerId = upsertPlayer(item.player());
                  LineupRole role =
                      item.missingReason() != null
                          ? LineupRole.MISSING
                          : item.substitute() ? LineupRole.SUBSTITUTE : LineupRole.STARTER;
                  return new LineupPlayerInput(
                      playerId,
                      role,
                      item.position(),
                      item.shirtNumber(),
                      item.captain(),
                      item.missingReason());
                })
            .toList();
    return new TeamLineupInput(teamId, source.formation(), confirmed, playerInputs);
  }

  private Long upsertPlayer(SofaScorePlayerSnapshot player) {
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

  private Long findCanonicalMatchId(long eventId) {
    if (eventId <= 0) throw new IllegalArgumentException("sofaScoreEventId must be positive");
    return identities
        .findCanonicalId(ResourceType.MATCH, PROVIDER, Long.toString(eventId))
        .orElseThrow(
            () -> new IllegalStateException("SofaScore event is not mapped; sync matches first"));
  }

  private boolean empty(SofaScoreLineupSnapshot.TeamLineup lineup) {
    return lineup == null || lineup.players().isEmpty();
  }

  public record LineupSyncResult(
      long sofaScoreEventId, long canonicalMatchId, boolean confirmed, int storedPlayers) {}
}
