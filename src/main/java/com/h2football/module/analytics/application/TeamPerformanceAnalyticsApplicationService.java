package com.h2football.module.analytics.application;

import com.h2football.module.analytics.application.TeamPerformanceCalculator.TeamPerformance;
import com.h2football.module.analytics.domain.model.TeamSeasonStatistic;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeamPerformanceAnalyticsApplicationService {
  private static final int SCALE = 3;

  private final TeamSeasonStatisticsApplicationService seasonStatistics;
  private final MatchRepository matches;
  private final MatchStatisticRepository matchStatistics;
  private final TeamPerformanceCalculator calculator;

  public TeamPerformanceAnalyticsApplicationService(
      TeamSeasonStatisticsApplicationService seasonStatistics,
      MatchRepository matches,
      MatchStatisticRepository matchStatistics,
      TeamPerformanceCalculator calculator) {
    this.seasonStatistics = seasonStatistics;
    this.matches = matches;
    this.matchStatistics = matchStatistics;
    this.calculator = calculator;
  }

  public List<TeamPerformanceResponse> findBySeason(Long seasonId, Provider provider) {
    List<Match> seasonMatches = matches.findAllBySeasonIdOrderByKickoffAtAsc(seasonId);
    return seasonStatistics.findBySeason(seasonId, provider).stream()
        .collect(
            java.util.stream.Collectors.groupingBy(
                item -> item.getTeam().getId(),
                java.util.LinkedHashMap::new,
                java.util.stream.Collectors.toList()))
        .entrySet()
        .stream()
        .map(
            entry ->
                toResponse(
                    entry.getKey(),
                    entry.getValue().get(0).getTeam().getName(),
                    seasonId,
                    provider,
                    entry.getValue(),
                    seasonMatches))
        .sorted(Comparator.comparing(TeamPerformanceResponse::teamName))
        .toList();
  }

  public TeamPerformanceResponse findByTeam(Long seasonId, Long teamId, Provider provider) {
    List<TeamSeasonStatistic> statistics = seasonStatistics.findByTeam(seasonId, teamId, provider);
    if (statistics.isEmpty()) {
      throw new IllegalArgumentException(
          "No team season statistics found for team " + teamId + " in season " + seasonId);
    }
    return toResponse(
        teamId,
        statistics.get(0).getTeam().getName(),
        seasonId,
        provider,
        statistics,
        matches.findAllBySeasonIdOrderByKickoffAtAsc(seasonId));
  }

  private TeamPerformanceResponse toResponse(
      Long teamId,
      String teamName,
      Long seasonId,
      Provider provider,
      List<TeamSeasonStatistic> statistics,
      List<Match> seasonMatches) {
    List<Match> completedMatches =
        seasonMatches.stream()
            .filter(match -> match.getStatus() == MatchStatus.FINISHED)
            .filter(match -> match.getHomeScore() != null && match.getAwayScore() != null)
            .filter(
                match ->
                    match.getHomeTeam().getId().equals(teamId)
                        || match.getAwayTeam().getId().equals(teamId))
            .toList();

    long goalsFor = 0;
    long goalsAgainst = 0;
    long homeMatches = 0;
    long awayMatches = 0;
    long homeGoalsFor = 0;
    long homeGoalsAgainst = 0;
    long awayGoalsFor = 0;
    long awayGoalsAgainst = 0;
    for (Match match : completedMatches) {
      boolean home = match.getHomeTeam().getId().equals(teamId);
      long scored = home ? match.getHomeScore() : match.getAwayScore();
      long conceded = home ? match.getAwayScore() : match.getHomeScore();
      goalsFor += scored;
      goalsAgainst += conceded;
      if (home) {
        homeMatches++;
        homeGoalsFor += scored;
        homeGoalsAgainst += conceded;
      } else {
        awayMatches++;
        awayGoalsFor += scored;
        awayGoalsAgainst += conceded;
      }
    }

    BigDecimal expectedGoalsAgainst =
        matchStatistics.averageExpectedGoalsAgainstBySeasonAndTeam(
            seasonId, provider.name(), teamId);
    TeamPerformance performance = calculator.calculate(statistics, expectedGoalsAgainst);
    return new TeamPerformanceResponse(
        seasonId,
        teamId,
        teamName,
        provider,
        (long) completedMatches.size(),
        goalsFor,
        goalsAgainst,
        average(goalsFor, completedMatches.size()),
        average(goalsAgainst, completedMatches.size()),
        performance.expectedGoalsFor(),
        performance.expectedGoalsAgainst(),
        performance.expectedGoalsDifference(),
        performance.shotsPerMatch(),
        performance.shotsOnTargetPerMatch(),
        performance.possessionPercent(),
        performance.shotAccuracyPercent(),
        split(homeMatches, homeGoalsFor, homeGoalsAgainst),
        split(awayMatches, awayGoalsFor, awayGoalsAgainst));
  }

  private BigDecimal average(long total, long count) {
    return count == 0
        ? null
        : BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
  }

  private VenueSplit split(long matches, long goalsFor, long goalsAgainst) {
    return new VenueSplit(
        matches,
        goalsFor,
        goalsAgainst,
        average(goalsFor, matches),
        average(goalsAgainst, matches));
  }

  public record VenueSplit(
      long matchesPlayed,
      long goalsFor,
      long goalsAgainst,
      BigDecimal goalsForPerMatch,
      BigDecimal goalsAgainstPerMatch) {}

  public record TeamPerformanceResponse(
      Long seasonId,
      Long teamId,
      String teamName,
      Provider provider,
      Long matchesPlayed,
      Long goalsFor,
      Long goalsAgainst,
      BigDecimal goalsForPerMatch,
      BigDecimal goalsAgainstPerMatch,
      BigDecimal expectedGoalsForPerMatch,
      BigDecimal expectedGoalsAgainstPerMatch,
      BigDecimal expectedGoalsDifferencePerMatch,
      BigDecimal shotsPerMatch,
      BigDecimal shotsOnTargetPerMatch,
      BigDecimal possessionPercent,
      BigDecimal shotAccuracyPercent,
      VenueSplit home,
      VenueSplit away) {}
}
