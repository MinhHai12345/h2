package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2football.module.analytics.domain.model.TeamSeasonStatistic;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamPerformanceAnalyticsApplicationServiceTest {
  private TeamSeasonStatisticsApplicationService seasonStatistics;
  private MatchRepository matches;
  private MatchStatisticRepository matchStatistics;
  private TeamPerformanceAnalyticsApplicationService service;

  @BeforeEach
  void setUp() {
    seasonStatistics = mock(TeamSeasonStatisticsApplicationService.class);
    matches = mock(MatchRepository.class);
    matchStatistics = mock(MatchStatisticRepository.class);
    service =
        new TeamPerformanceAnalyticsApplicationService(
            seasonStatistics, matches, matchStatistics, new TeamPerformanceCalculator());
  }

  @Test
  void calculatesGoalsAndHomeAwaySplitsFromFinishedMatches() {
    TeamSeasonStatistic expectedGoals = mock(TeamSeasonStatistic.class);
    TeamSeasonStatistic shots = mock(TeamSeasonStatistic.class);
    var team = mock(com.h2football.module.masterdata.domain.model.Team.class);
    when(team.getId()).thenReturn(10L);
    when(team.getName()).thenReturn("North FC");
    when(expectedGoals.getTeam()).thenReturn(team);
    when(expectedGoals.getPeriod()).thenReturn("ALL");
    when(expectedGoals.getStatisticKey()).thenReturn("expectedGoals");
    when(expectedGoals.getAverageValue()).thenReturn(new BigDecimal("1.7"));
    when(shots.getPeriod()).thenReturn("ALL");
    when(shots.getStatisticKey()).thenReturn("totalShots");
    when(shots.getAverageValue()).thenReturn(new BigDecimal("13"));

    Match homeMatch = finishedMatch(10L, 20L, 2, 1);
    Match awayMatch = finishedMatch(30L, 10L, 0, 1);
    when(seasonStatistics.findByTeam(5L, 10L, Provider.SOFASCORE))
        .thenReturn(List.of(expectedGoals, shots));
    when(matches.findAllBySeasonIdOrderByKickoffAtAsc(5L))
        .thenReturn(List.of(homeMatch, awayMatch));
    when(matchStatistics.averageExpectedGoalsAgainstBySeasonAndTeam(5L, "SOFASCORE", 10L))
        .thenReturn(new BigDecimal("1.2"));

    var result = service.findByTeam(5L, 10L, Provider.SOFASCORE);

    assertThat(result.matchesPlayed()).isEqualTo(2);
    assertThat(result.goalsFor()).isEqualTo(3);
    assertThat(result.goalsAgainst()).isEqualTo(1);
    assertThat(result.goalsForPerMatch()).isEqualByComparingTo("1.500");
    assertThat(result.expectedGoalsDifferencePerMatch()).isEqualByComparingTo("0.500");
    assertThat(result.home().matchesPlayed()).isEqualTo(1);
    assertThat(result.home().goalsFor()).isEqualTo(2);
    assertThat(result.away().matchesPlayed()).isEqualTo(1);
    assertThat(result.away().goalsAgainst()).isEqualTo(0);
  }

  private Match finishedMatch(Long homeTeamId, Long awayTeamId, int homeScore, int awayScore) {
    Match match = mock(Match.class);
    var home = mock(com.h2football.module.masterdata.domain.model.Team.class);
    var away = mock(com.h2football.module.masterdata.domain.model.Team.class);
    when(home.getId()).thenReturn(homeTeamId);
    when(away.getId()).thenReturn(awayTeamId);
    when(match.getHomeTeam()).thenReturn(home);
    when(match.getAwayTeam()).thenReturn(away);
    when(match.getStatus()).thenReturn(MatchStatus.FINISHED);
    when(match.getHomeScore()).thenReturn(homeScore);
    when(match.getAwayScore()).thenReturn(awayScore);
    return match;
  }
}
