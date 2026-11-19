package com.h2football.module.workflow.application;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.analytics.application.MatchAnalyticsApplicationService;
import com.h2football.module.analytics.application.PlayerSeasonStatisticsApplicationService;
import com.h2football.module.analytics.application.TeamSeasonStatisticsApplicationService;
import com.h2football.module.analytics.domain.model.MatchAnalytics;
import com.h2football.module.analytics.domain.model.MatchAnalyticsStatus;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsRefreshStageHandlerTest {
  @Mock private MatchRepository matches;
  @Mock private TeamSeasonStatisticsApplicationService teamStatistics;
  @Mock private PlayerSeasonStatisticsApplicationService playerStatistics;
  @Mock private MatchAnalyticsApplicationService matchAnalytics;

  @Test
  void shouldRebuildSeasonAggregatesAndOnlyUpcomingMatchSnapshots() throws Exception {
    Instant now = Instant.parse("2026-11-19T10:00:00Z");
    Match upcoming = mock(Match.class);
    Match completed = mock(Match.class);
    MatchAnalytics refreshed = mock(MatchAnalytics.class);
    when(upcoming.getId()).thenReturn(1L);
    when(upcoming.getKickoffAt()).thenReturn(now.plusSeconds(3600));
    when(upcoming.getStatus()).thenReturn(MatchStatus.NOT_STARTED);
    when(completed.getKickoffAt()).thenReturn(now.minusSeconds(3600));
    when(completed.getStatus()).thenReturn(MatchStatus.FINISHED);
    when(matches.findAllBySeasonIdOrderByKickoffAtAsc(42L))
        .thenReturn(List.of(completed, upcoming));
    when(teamStatistics.rebuild(42L, Provider.SOFASCORE))
        .thenReturn(
            new TeamSeasonStatisticsApplicationService.RebuildResult(
                42L, Provider.SOFASCORE, 2, 12));
    when(playerStatistics.rebuild(42L, Provider.SOFASCORE))
        .thenReturn(
            new PlayerSeasonStatisticsApplicationService.RebuildResult(
                42L, Provider.SOFASCORE, 20, 20));
    when(matchAnalytics.rebuild(1L, Provider.SOFASCORE)).thenReturn(refreshed);
    when(refreshed.getStatus()).thenReturn(MatchAnalyticsStatus.READY);

    var handler =
        new AnalyticsRefreshStageHandler(
            new ObjectMapper(),
            matches,
            teamStatistics,
            playerStatistics,
            matchAnalytics,
            Clock.fixed(now, ZoneOffset.UTC));

    handler.execute(
        new WorkflowExecutionContext(
            99L,
            "SOFASCORE_SEASON_SYNC",
            "ANALYTICS_REFRESH",
            new ObjectMapper()
                .writeValueAsString(new SofaScoreSeasonWorkflowRequest("key", 17L, 52186L, 42L)),
            1));

    verify(teamStatistics).rebuild(42L, Provider.SOFASCORE);
    verify(playerStatistics).rebuild(42L, Provider.SOFASCORE);
    verify(matchAnalytics).rebuild(1L, Provider.SOFASCORE);
    verify(matchAnalytics, never()).rebuild(eq(2L), eq(Provider.SOFASCORE));
  }
}
