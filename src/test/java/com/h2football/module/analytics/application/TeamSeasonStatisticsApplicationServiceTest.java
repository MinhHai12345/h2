package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.analytics.domain.repository.TeamSeasonStatisticRepository;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import com.h2football.module.match.domain.repository.TeamSeasonStatisticProjection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamSeasonStatisticsApplicationServiceTest {
  private SeasonRepository seasons;
  private TeamRepository teams;
  private MatchStatisticRepository matchStatistics;
  private TeamSeasonStatisticRepository seasonStatistics;
  private TeamSeasonStatisticsApplicationService service;

  @BeforeEach
  void setUp() {
    seasons = mock(SeasonRepository.class);
    teams = mock(TeamRepository.class);
    matchStatistics = mock(MatchStatisticRepository.class);
    seasonStatistics = mock(TeamSeasonStatisticRepository.class);
    service =
        new TeamSeasonStatisticsApplicationService(
            seasons, teams, matchStatistics, seasonStatistics);
  }

  @Test
  void rebuildsAggregatesFromFinishedMatchStatistics() {
    Season season = mock(Season.class);
    Team team = new Team("North FC", mock(Country.class));
    TeamSeasonStatisticProjection projection = mock(TeamSeasonStatisticProjection.class);
    when(seasons.findById(5L)).thenReturn(Optional.of(season));
    when(matchStatistics.aggregateTeamStatisticsBySeason(5L, "SOFASCORE"))
        .thenReturn(List.of(projection));
    when(projection.getTeamId()).thenReturn(7L);
    when(projection.getPeriod()).thenReturn("ALL");
    when(projection.getGroupName()).thenReturn("Attack");
    when(projection.getStatisticKey()).thenReturn("total_shots");
    when(projection.getStatisticName()).thenReturn("Total shots");
    when(projection.getMatchesCount()).thenReturn(10L);
    when(projection.getTotalValue()).thenReturn(new BigDecimal("120"));
    when(projection.getAverageValue()).thenReturn(new BigDecimal("12"));
    when(teams.findById(7L)).thenReturn(Optional.of(team));

    var result = service.rebuild(5L, Provider.SOFASCORE);

    assertThat(result.aggregatedTeams()).isEqualTo(1);
    assertThat(result.storedStatistics()).isEqualTo(1);
    verify(seasonStatistics).deleteAllBySeasonIdAndProvider(5L, Provider.SOFASCORE);
    verify(seasonStatistics).saveAll(anyList());
  }

  @Test
  void doesNotDeleteExistingAggregatesWhenSourceIsEmpty() {
    when(seasons.findById(5L)).thenReturn(Optional.of(mock(Season.class)));
    when(matchStatistics.aggregateTeamStatisticsBySeason(5L, "SOFASCORE")).thenReturn(List.of());

    var result = service.rebuild(5L, Provider.SOFASCORE);

    assertThat(result.aggregatedTeams()).isZero();
    verify(seasonStatistics, never()).deleteAllBySeasonIdAndProvider(anyLong(), any());
    verify(seasonStatistics, never()).saveAll(anyList());
  }
}
