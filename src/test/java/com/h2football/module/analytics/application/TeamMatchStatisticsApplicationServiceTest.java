package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatistic;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamMatchStatisticsApplicationServiceTest {
  private MatchRepository matches;
  private TeamRepository teams;
  private MatchStatisticRepository statistics;
  private TeamMatchStatisticsApplicationService service;
  private Match match;
  private Team home;
  private Team away;

  @BeforeEach
  void setUp() {
    matches = mock(MatchRepository.class);
    teams = mock(TeamRepository.class);
    statistics = mock(MatchStatisticRepository.class);
    service = new TeamMatchStatisticsApplicationService(matches, teams, statistics);
    match = mock(Match.class);
    home = mock(Team.class);
    away = mock(Team.class);
    when(home.getId()).thenReturn(10L);
    when(away.getId()).thenReturn(20L);
    when(match.getHomeTeam()).thenReturn(home);
    when(match.getAwayTeam()).thenReturn(away);
    when(matches.findById(99L)).thenReturn(Optional.of(match));
    when(teams.existsById(10L)).thenReturn(true);
    when(teams.existsById(20L)).thenReturn(true);
  }

  @Test
  void returnsTeamAndOpponentValuesFromHomePerspective() {
    var statistic =
        new MatchStatistic(
            match,
            Provider.SOFASCORE,
            "ALL",
            "Attack",
            "total_shots",
            "Total shots",
            "14",
            "8",
            new BigDecimal("14"),
            new BigDecimal("8"));
    when(statistics.findAllByMatchIdOrderByPeriodAscGroupNameAscStatisticKeyAsc(99L))
        .thenReturn(List.of(statistic));

    var result = service.findByMatch(10L, 99L, Provider.SOFASCORE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).teamValue()).isEqualByComparingTo("14");
    assertThat(result.get(0).opponentValue()).isEqualByComparingTo("8");
  }

  @Test
  void reversesValuesFromAwayPerspective() {
    var statistic =
        new MatchStatistic(
            match,
            Provider.SOFASCORE,
            "ALL",
            "Attack",
            "total_shots",
            "Total shots",
            "14",
            "8",
            new BigDecimal("14"),
            new BigDecimal("8"));
    when(statistics.findAllByMatchIdOrderByPeriodAscGroupNameAscStatisticKeyAsc(99L))
        .thenReturn(List.of(statistic));

    var result = service.findByMatch(20L, 99L, Provider.SOFASCORE);

    assertThat(result.get(0).teamValue()).isEqualByComparingTo("8");
    assertThat(result.get(0).opponentValue()).isEqualByComparingTo("14");
  }

  @Test
  void rejectsTeamThatDidNotParticipateInMatch() {
    when(teams.existsById(30L)).thenReturn(true);
    assertThatThrownBy(() -> service.findByMatch(30L, 99L, Provider.SOFASCORE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not participate");
  }
}
