package com.h2football.module.analytics.application;

import com.h2football.module.analytics.domain.model.TeamSeasonStatistic;
import com.h2football.module.analytics.domain.repository.TeamSeasonStatisticRepository;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import com.h2football.module.match.domain.repository.TeamSeasonStatisticProjection;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TeamSeasonStatisticsApplicationService {
  private final SeasonRepository seasons;
  private final TeamRepository teams;
  private final MatchStatisticRepository matchStatistics;
  private final TeamSeasonStatisticRepository seasonStatistics;

  public TeamSeasonStatisticsApplicationService(
      SeasonRepository seasons,
      TeamRepository teams,
      MatchStatisticRepository matchStatistics,
      TeamSeasonStatisticRepository seasonStatistics) {
    this.seasons = seasons;
    this.teams = teams;
    this.matchStatistics = matchStatistics;
    this.seasonStatistics = seasonStatistics;
  }

  public RebuildResult rebuild(Long seasonId, Provider provider) {
    validate(seasonId, provider);
    var season =
        seasons
            .findById(seasonId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown season " + seasonId));

    List<TeamSeasonStatisticProjection> aggregates =
        matchStatistics.aggregateTeamStatisticsBySeason(seasonId, provider.name());
    if (aggregates.isEmpty()) {
      return new RebuildResult(seasonId, provider, 0, 0);
    }

    List<TeamSeasonStatistic> replacement =
        aggregates.stream()
            .map(
                aggregate -> {
                  var team =
                      teams
                          .findById(aggregate.getTeamId())
                          .orElseThrow(
                              () ->
                                  new IllegalStateException(
                                      "Match statistics reference missing team "
                                          + aggregate.getTeamId()));
                  return new TeamSeasonStatistic(
                      season,
                      team,
                      provider,
                      aggregate.getPeriod(),
                      aggregate.getGroupName(),
                      aggregate.getStatisticKey(),
                      aggregate.getStatisticName(),
                      aggregate.getMatchesCount(),
                      aggregate.getTotalValue(),
                      aggregate.getAverageValue());
                })
            .toList();

    seasonStatistics.deleteAllBySeasonIdAndProvider(seasonId, provider);
    seasonStatistics.saveAll(replacement);
    int teamCount =
        (int) aggregates.stream().map(TeamSeasonStatisticProjection::getTeamId).distinct().count();
    return new RebuildResult(seasonId, provider, teamCount, replacement.size());
  }

  @Transactional(readOnly = true)
  public List<TeamSeasonStatistic> findBySeason(Long seasonId, Provider provider) {
    validate(seasonId, provider);
    if (!seasons.existsById(seasonId)) {
      throw new IllegalArgumentException("Unknown season " + seasonId);
    }
    return seasonStatistics.findAllBySeasonIdAndProvider(seasonId, provider).stream()
        .sorted(
            Comparator.comparing((TeamSeasonStatistic item) -> item.getTeam().getName())
                .thenComparing(TeamSeasonStatistic::getPeriod)
                .thenComparing(TeamSeasonStatistic::getGroupName)
                .thenComparing(TeamSeasonStatistic::getStatisticKey))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TeamSeasonStatistic> findByTeam(Long seasonId, Long teamId, Provider provider) {
    validate(seasonId, provider);
    if (teamId == null || teamId <= 0) {
      throw new IllegalArgumentException("teamId must be positive");
    }
    if (!seasons.existsById(seasonId)) {
      throw new IllegalArgumentException("Unknown season " + seasonId);
    }
    if (!teams.existsById(teamId)) {
      throw new IllegalArgumentException("Unknown team " + teamId);
    }
    return seasonStatistics
        .findAllBySeasonIdAndTeamIdAndProviderOrderByPeriodAscGroupNameAscStatisticKeyAsc(
            seasonId, teamId, provider);
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
      Long seasonId, Provider provider, int aggregatedTeams, int storedStatistics) {}
}
