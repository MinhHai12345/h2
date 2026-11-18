package com.h2football.module.analytics.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.match.domain.repository.MatchPlayerStatisticRepository;
import com.h2football.module.match.domain.repository.SeasonPlayerStatisticProjection;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic.PlayerSeasonStatisticSnapshot;
import com.h2football.module.player.domain.repository.PlayerRepository;
import com.h2football.module.player.domain.repository.PlayerSeasonStatisticRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlayerSeasonStatisticsApplicationService {
  private final SeasonRepository seasons;
  private final PlayerRepository players;
  private final MatchPlayerStatisticRepository matchStatistics;
  private final PlayerSeasonStatisticRepository seasonStatistics;

  public PlayerSeasonStatisticsApplicationService(
      SeasonRepository seasons,
      PlayerRepository players,
      MatchPlayerStatisticRepository matchStatistics,
      PlayerSeasonStatisticRepository seasonStatistics) {
    this.seasons = seasons;
    this.players = players;
    this.matchStatistics = matchStatistics;
    this.seasonStatistics = seasonStatistics;
  }

  public RebuildResult rebuild(Long seasonId, Provider provider) {
    validate(seasonId, provider);
    var season =
        seasons
            .findById(seasonId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown season " + seasonId));

    List<SeasonPlayerStatisticProjection> aggregates =
        matchStatistics.aggregateBySeasonAndProvider(seasonId, provider);
    int stored = 0;
    for (SeasonPlayerStatisticProjection aggregate : aggregates) {
      var player =
          players
              .findById(aggregate.getPlayerId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Match statistics reference missing player " + aggregate.getPlayerId()));
      PlayerSeasonStatisticSnapshot snapshot = toSnapshot(aggregate);
      PlayerSeasonStatistic statistic =
          seasonStatistics
              .findBySeasonIdAndPlayerIdAndProvider(seasonId, aggregate.getPlayerId(), provider)
              .orElseGet(() -> new PlayerSeasonStatistic(season, player, provider, snapshot));
      statistic.updateSnapshot(snapshot);
      seasonStatistics.save(statistic);
      stored++;
    }
    return new RebuildResult(seasonId, provider, aggregates.size(), stored);
  }

  @Transactional(readOnly = true)
  public List<PlayerSeasonStatistic> findBySeason(Long seasonId, Provider provider) {
    validate(seasonId, provider);
    if (!seasons.existsById(seasonId)) {
      throw new IllegalArgumentException("Unknown season " + seasonId);
    }
    return seasonStatistics.findAllBySeasonIdAndProviderOrderByGoalsDescAverageRatingDesc(
        seasonId, provider);
  }

  private PlayerSeasonStatisticSnapshot toSnapshot(SeasonPlayerStatisticProjection aggregate) {
    return new PlayerSeasonStatisticSnapshot(
        zero(aggregate.getAppearances()),
        zero(aggregate.getMinutesPlayed()),
        zero(aggregate.getGoals()),
        zero(aggregate.getAssists()),
        zero(aggregate.getTotalShots()),
        zero(aggregate.getShotsOnTarget()),
        zero(aggregate.getAccuratePasses()),
        zero(aggregate.getTotalPasses()),
        zero(aggregate.getKeyPasses()),
        zero(aggregate.getTackles()),
        zero(aggregate.getInterceptions()),
        zero(aggregate.getClearances()),
        aggregate.getExpectedGoals(),
        aggregate.getExpectedAssists(),
        aggregate.getAverageRating() == null
            ? null
            : BigDecimal.valueOf(aggregate.getAverageRating())
                .setScale(3, java.math.RoundingMode.HALF_UP));
  }

  private Long zero(Long value) {
    return value == null ? 0L : value;
  }

  private void validate(Long seasonId, Provider provider) {
    if (seasonId == null || seasonId <= 0) {
      throw new IllegalArgumentException("seasonId must be positive");
    }
    if (provider == null) {
      throw new IllegalArgumentException("provider must not be null");
    }
  }

  public record RebuildResult(
      Long seasonId, Provider provider, int aggregatedPlayers, int storedPlayers) {}
}
