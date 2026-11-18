package com.h2football.module.ingestion.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.match.application.MatchPlayerStatisticsApplicationService;
import com.h2football.module.match.application.MatchPlayerStatisticsApplicationService.PlayerStatisticInput;
import com.h2football.module.player.application.PlayerApplicationService;
import com.h2football.module.player.application.PlayerApplicationService.PlayerInput;
import com.h2football.module.provider.sofascore.SofaScoreMatchEventProvider;
import com.h2football.module.provider.sofascore.model.SofaScoreLineupSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScorePlayerStatisticsSnapshot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SofaScorePlayerMatchStatisticsIngestionService {
  private static final Provider PROVIDER = Provider.SOFASCORE;

  private final SofaScoreMatchEventProvider provider;
  private final ExternalIdentityService identities;
  private final PlayerApplicationService players;
  private final MatchPlayerStatisticsApplicationService statistics;

  public SofaScorePlayerMatchStatisticsIngestionService(
      SofaScoreMatchEventProvider provider,
      ExternalIdentityService identities,
      PlayerApplicationService players,
      MatchPlayerStatisticsApplicationService statistics) {
    this.provider = provider;
    this.identities = identities;
    this.players = players;
    this.statistics = statistics;
  }

  public PlayerStatisticsSyncResult sync(long sofaScoreEventId) {
    if (sofaScoreEventId <= 0) {
      throw new IllegalArgumentException("sofaScoreEventId must be positive");
    }
    Long matchId =
        identities
            .findCanonicalId(ResourceType.MATCH, PROVIDER, Long.toString(sofaScoreEventId))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "SofaScore event is not mapped; sync matches before player statistics"));

    SofaScoreLineupSnapshot snapshot = provider.fetchLineups(sofaScoreEventId);
    if (snapshot == null) {
      return new PlayerStatisticsSyncResult(sofaScoreEventId, matchId, 0, 0);
    }

    List<PlayerStatisticInput> inputs = new ArrayList<>();
    collect(snapshot.home(), inputs);
    collect(snapshot.away(), inputs);
    if (inputs.isEmpty()) {
      return new PlayerStatisticsSyncResult(sofaScoreEventId, matchId, 0, 0);
    }
    int stored = statistics.upsertFromProvider(matchId, PROVIDER, inputs);
    return new PlayerStatisticsSyncResult(sofaScoreEventId, matchId, inputs.size(), stored);
  }

  private void collect(
      SofaScoreLineupSnapshot.TeamLineup team, List<PlayerStatisticInput> destination) {
    if (team == null) {
      return;
    }
    for (SofaScoreLineupSnapshot.LineupPlayer lineupPlayer : team.players()) {
      SofaScorePlayerStatisticsSnapshot source = lineupPlayer.statistics();
      if (source == null || source.values().isEmpty()) {
        continue;
      }
      var profile = lineupPlayer.player();
      Long playerId =
          players.upsertFromProvider(
              PROVIDER,
              profile.externalId(),
              new PlayerInput(
                  profile.name(),
                  profile.shortName(),
                  profile.slug(),
                  profile.position(),
                  profile.countryCode(),
                  profile.countryName()));
      destination.add(
          new PlayerStatisticInput(
              playerId,
              decimal(source, "rating"),
              integer(source, "minutesPlayed"),
              integer(source, "goals"),
              integer(source, "assists"),
              integer(source, "totalShots"),
              integer(source, "shotsOnTarget"),
              integer(source, "accuratePasses"),
              integer(source, "totalPasses"),
              integer(source, "keyPasses"),
              integer(source, "tackles"),
              integer(source, "interceptions"),
              integer(source, "clearances"),
              decimal(source, "expectedGoals"),
              decimal(source, "expectedAssists"),
              source.rawJson()));
    }
  }

  private BigDecimal decimal(SofaScorePlayerStatisticsSnapshot source, String key) {
    String value = source.value(key);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private Integer integer(SofaScorePlayerStatisticsSnapshot source, String key) {
    BigDecimal value = decimal(source, key);
    if (value == null) {
      return null;
    }
    try {
      return value.intValueExact();
    } catch (ArithmeticException exception) {
      return null;
    }
  }

  public record PlayerStatisticsSyncResult(
      long sofaScoreEventId, long canonicalMatchId, int discoveredPlayers, int storedPlayers) {}
}
