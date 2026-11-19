package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2football.module.analytics.application.PlayerImpactApplicationService.MatchPlayerImpactResponse;
import com.h2football.module.analytics.application.PlayerImpactApplicationService.TeamImpact;
import com.h2football.module.analytics.domain.model.MatchAnalytics;
import com.h2football.module.analytics.domain.model.MatchAnalyticsStatus;
import com.h2football.module.analytics.domain.repository.MatchAnalyticsRepository;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatistic;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchAnalyticsApplicationServiceTest {
  private MatchRepository matches;
  private MatchStatisticRepository matchStatistics;
  private MatchAnalyticsRepository analytics;
  private PlayerImpactApplicationService playerImpact;
  private ModelEvaluationApplicationService modelEvaluation;
  private MatchAnalyticsApplicationService service;
  private Team home;
  private Team away;
  private Match target;
  private Match firstHistory;
  private Match secondHistory;

  @BeforeEach
  void setUp() {
    matches = mock(MatchRepository.class);
    matchStatistics = mock(MatchStatisticRepository.class);
    analytics = mock(MatchAnalyticsRepository.class);
    playerImpact = mock(PlayerImpactApplicationService.class);
    modelEvaluation = mock(ModelEvaluationApplicationService.class);
    service =
        new MatchAnalyticsApplicationService(
            matches,
            matchStatistics,
            analytics,
            new MatchAnalyticsCalculator(),
            playerImpact,
            modelEvaluation,
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    home = team(10L, "Home FC");
    away = team(20L, "Away FC");
    Season season = mock(Season.class);
    when(season.getId()).thenReturn(5L);
    target = match(season, home, away, "2025-12-20T18:00:00Z", MatchStatus.NOT_STARTED, null, null);
    firstHistory = match(season, home, away, "2025-12-01T18:00:00Z", MatchStatus.FINISHED, 2, 0);
    secondHistory = match(season, away, home, "2025-11-20T18:00:00Z", MatchStatus.FINISHED, 1, 0);
    when(matches.findById(100L)).thenReturn(Optional.of(target));
    when(matches.findAllBySeasonIdOrderByKickoffAtAsc(5L))
        .thenReturn(List.of(secondHistory, firstHistory, target));
    when(analytics.findByMatchIdAndProvider(100L, Provider.SOFASCORE)).thenReturn(Optional.empty());
    when(analytics.save(any(MatchAnalytics.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void buildsAndPersistsPreMatchAnalyticsWithoutUsingTargetMatchData() {
    List<MatchStatistic> historicalStatistics =
        List.of(statistic(firstHistory, "1.6", "0.7"), statistic(secondHistory, "0.8", "1.4"));
    when(matchStatistics
            .findAllByMatchIdInAndProviderOrderByMatchIdAscPeriodAscGroupNameAscStatisticKeyAsc(
                List.of(1L, 2L), Provider.SOFASCORE))
        .thenReturn(historicalStatistics);
    when(playerImpact.calculate(100L, Provider.SOFASCORE))
        .thenReturn(
            impact(
                new BigDecimal("0.70"),
                new BigDecimal("0.20"),
                new BigDecimal("100"),
                new BigDecimal("0.30"),
                new BigDecimal("0.60"),
                new BigDecimal("100")));

    MatchAnalytics result = service.rebuild(100L, Provider.SOFASCORE);

    assertThat(result.getStatus()).isEqualTo(MatchAnalyticsStatus.READY);
    assertThat(result.getHomeExpectedGoals()).isNotNull().isPositive();
    assertThat(result.getAwayExpectedGoals()).isNotNull().isPositive();
    assertThat(
            result
                .getHomeWinProbability()
                .add(result.getDrawProbability())
                .add(result.getAwayWinProbability()))
        .isEqualByComparingTo("1.00000000");
    assertThat(result.getMatchAttractivenessIndex())
        .isBetween(BigDecimal.ZERO, new BigDecimal("100"));
    assertThat(result.getHomeHistoryMatches()).isEqualTo(2);
    assertThat(result.getAwayHistoryMatches()).isEqualTo(2);
    assertThat(result.getModelVersion()).isEqualTo("poisson-ema-player-impact-v2");
  }

  @Test
  void keepsBaselineWhenConfirmedLineupImpactIsUnavailable() {
    List<MatchStatistic> historicalStatistics =
        List.of(statistic(firstHistory, "1.6", "0.7"), statistic(secondHistory, "0.8", "1.4"));
    when(matchStatistics
            .findAllByMatchIdInAndProviderOrderByMatchIdAscPeriodAscGroupNameAscStatisticKeyAsc(
                List.of(1L, 2L), Provider.SOFASCORE))
        .thenReturn(historicalStatistics);
    when(playerImpact.calculate(100L, Provider.SOFASCORE)).thenReturn(null);

    MatchAnalytics result = service.rebuild(100L, Provider.SOFASCORE);

    assertThat(result.getStatus()).isEqualTo(MatchAnalyticsStatus.READY);
    assertThat(result.getModelVersion()).isEqualTo("poisson-ema-player-impact-v2");
  }

  @Test
  void persistsInsufficientDataInsteadOfInventingProbabilities() {
    when(matchStatistics
            .findAllByMatchIdInAndProviderOrderByMatchIdAscPeriodAscGroupNameAscStatisticKeyAsc(
                List.of(1L, 2L), Provider.SOFASCORE))
        .thenReturn(List.of());

    MatchAnalytics result = service.rebuild(100L, Provider.SOFASCORE);

    assertThat(result.getStatus()).isEqualTo(MatchAnalyticsStatus.INSUFFICIENT_DATA);
    assertThat(result.getHomeExpectedGoals()).isNull();
    assertThat(result.getHomeWinProbability()).isNull();
    assertThat(result.getMatchAttractivenessIndex()).isNull();
    assertThat(result.getInsufficientDataReason()).contains("expectedGoals");
  }

  private MatchPlayerImpactResponse impact(
      BigDecimal homeAttack,
      BigDecimal homeDefense,
      BigDecimal homeCoverage,
      BigDecimal awayAttack,
      BigDecimal awayDefense,
      BigDecimal awayCoverage) {
    return new MatchPlayerImpactResponse(
        100L,
        Provider.SOFASCORE,
        Instant.parse("2025-12-20T18:00:00Z"),
        teamImpact(10L, "Home FC", homeAttack, homeDefense, homeCoverage),
        teamImpact(20L, "Away FC", awayAttack, awayDefense, awayCoverage),
        "player-impact-v1");
  }

  private TeamImpact teamImpact(
      Long teamId, String teamName, BigDecimal attack, BigDecimal defense, BigDecimal coverage) {
    return new TeamImpact(
        teamId,
        teamName,
        true,
        "4-3-3",
        11,
        11,
        new BigDecimal("70.000"),
        attack,
        defense,
        coverage,
        List.of());
  }

  private Team team(Long id, String name) {
    Team team = mock(Team.class);
    when(team.getId()).thenReturn(id);
    when(team.getName()).thenReturn(name);
    return team;
  }

  private Match match(
      Season season,
      Team homeTeam,
      Team awayTeam,
      String kickoff,
      MatchStatus status,
      Integer homeScore,
      Integer awayScore) {
    Match match = mock(Match.class);
    when(match.getId())
        .thenReturn(
            kickoff.startsWith("2025-12-01") ? 1L : kickoff.startsWith("2025-11-20") ? 2L : 100L);
    when(match.getSeason()).thenReturn(season);
    when(match.getHomeTeam()).thenReturn(homeTeam);
    when(match.getAwayTeam()).thenReturn(awayTeam);
    when(match.getKickoffAt()).thenReturn(Instant.parse(kickoff));
    when(match.getStatus()).thenReturn(status);
    when(match.getHomeScore()).thenReturn(homeScore);
    when(match.getAwayScore()).thenReturn(awayScore);
    return match;
  }

  private MatchStatistic statistic(Match match, String homeValue, String awayValue) {
    MatchStatistic statistic = mock(MatchStatistic.class);
    when(statistic.getMatch()).thenReturn(match);
    when(statistic.getPeriod()).thenReturn("ALL");
    when(statistic.getStatisticKey()).thenReturn("expectedGoals");
    when(statistic.getHomeValue()).thenReturn(new BigDecimal(homeValue));
    when(statistic.getAwayValue()).thenReturn(new BigDecimal(awayValue));
    return statistic;
  }
}
