package com.h2football.module.analytics.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.analytics.application.MatchAnalyticsApplicationService;
import com.h2football.module.analytics.application.ModelEvaluationApplicationService;
import com.h2football.module.analytics.domain.model.MatchAnalytics;
import com.h2football.module.analytics.domain.repository.MatchAnalyticsRepository;
import com.h2football.module.analytics.domain.repository.MatchPredictionSnapshotRepository;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsSchedulingApplicationServiceTest {
  private static final Instant NOW = Instant.parse("2026-05-10T12:00:00Z");

  private MatchRepository matches;
  private MatchAnalyticsRepository analytics;
  private MatchPredictionSnapshotRepository snapshots;
  private MatchAnalyticsApplicationService matchAnalytics;
  private ModelEvaluationApplicationService modelEvaluation;
  private AnalyticsSchedulerProperties properties;
  private AnalyticsSchedulingApplicationService service;

  @BeforeEach
  void setUp() {
    matches = mock(MatchRepository.class);
    analytics = mock(MatchAnalyticsRepository.class);
    snapshots = mock(MatchPredictionSnapshotRepository.class);
    matchAnalytics = mock(MatchAnalyticsApplicationService.class);
    modelEvaluation = mock(ModelEvaluationApplicationService.class);
    properties = new AnalyticsSchedulerProperties();
    properties.setProviders(List.of(Provider.SOFASCORE, Provider.UNDERSTAT));
    service =
        new AnalyticsSchedulingApplicationService(
            matches,
            analytics,
            snapshots,
            matchAnalytics,
            modelEvaluation,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void refreshesUpcomingMatchesForEachProviderWhenNoFreshAnalyticsExists() {
    Match first = match(1L);
    Match second = match(2L);
    when(matches.findAllByStatusAndKickoffAtBetweenOrderByKickoffAtAsc(
            MatchStatus.NOT_STARTED, NOW, NOW.plusSeconds(72 * 3600L)))
        .thenReturn(List.of(first, second));
    when(analytics.findByMatchIdAndProvider(anyLong(), any())).thenReturn(Optional.empty());

    var result = service.refreshUpcomingMatches();

    assertThat(result.candidateMatches()).isEqualTo(2);
    assertThat(result.providerRuns()).isEqualTo(4);
    assertThat(result.rebuiltMatches()).isEqualTo(4);
    assertThat(result.skippedFreshMatches()).isZero();
    assertThat(result.failedRuns()).isZero();
    verify(matchAnalytics).rebuild(1L, Provider.SOFASCORE);
    verify(matchAnalytics).rebuild(2L, Provider.UNDERSTAT);
  }

  @Test
  void skipsAnalyticsThatWereRefreshedWithinConfiguredWindow() {
    Match fixture = match(3L);
    MatchAnalytics current = mock(MatchAnalytics.class);
    when(matches.findAllByStatusAndKickoffAtBetweenOrderByKickoffAtAsc(
            MatchStatus.NOT_STARTED, NOW, NOW.plusSeconds(72 * 3600L)))
        .thenReturn(List.of(fixture));
    when(analytics.findByMatchIdAndProvider(3L, Provider.SOFASCORE))
        .thenReturn(Optional.of(current));
    when(analytics.findByMatchIdAndProvider(3L, Provider.UNDERSTAT)).thenReturn(Optional.empty());
    when(current.getComputedAt()).thenReturn(NOW.minusSeconds(60));

    var result = service.refreshUpcomingMatches();

    assertThat(result.rebuiltMatches()).isEqualTo(1);
    assertThat(result.skippedFreshMatches()).isEqualTo(1);
    verify(matchAnalytics, never()).rebuild(3L, Provider.SOFASCORE);
    verify(matchAnalytics).rebuild(3L, Provider.UNDERSTAT);
  }

  @Test
  void isolatesFailureToTheAffectedMatchAndProvider() {
    Match first = match(4L);
    Match second = match(5L);
    when(matches.findAllByStatusAndKickoffAtBetweenOrderByKickoffAtAsc(
            MatchStatus.NOT_STARTED, NOW, NOW.plusSeconds(72 * 3600L)))
        .thenReturn(List.of(first, second));
    when(analytics.findByMatchIdAndProvider(anyLong(), any())).thenReturn(Optional.empty());
    org.mockito.Mockito.doThrow(new IllegalStateException("simulated failure"))
        .when(matchAnalytics)
        .rebuild(4L, Provider.SOFASCORE);

    var result = service.refreshUpcomingMatches();

    assertThat(result.rebuiltMatches()).isEqualTo(3);
    assertThat(result.failedRuns()).isEqualTo(1);
    verify(matchAnalytics).rebuild(5L, Provider.SOFASCORE);
    verify(matchAnalytics).rebuild(4L, Provider.UNDERSTAT);
  }

  @Test
  void evaluatesDistinctSeasonsContainingFinishedPredictionSnapshots() {
    when(snapshots.findDistinctSeasonIdsByProviderAndMatchStatus(
            Provider.SOFASCORE, MatchStatus.FINISHED))
        .thenReturn(List.of(5L, 6L));
    when(snapshots.findDistinctSeasonIdsByProviderAndMatchStatus(
            Provider.UNDERSTAT, MatchStatus.FINISHED))
        .thenReturn(List.of(5L));
    when(modelEvaluation.evaluateSeason(5L, Provider.SOFASCORE))
        .thenReturn(List.of(report(5L, Provider.SOFASCORE, 2)));
    when(modelEvaluation.evaluateSeason(6L, Provider.SOFASCORE))
        .thenReturn(List.of(report(6L, Provider.SOFASCORE, 1)));
    when(modelEvaluation.evaluateSeason(5L, Provider.UNDERSTAT))
        .thenReturn(List.of(report(5L, Provider.UNDERSTAT, 3)));

    var result = service.evaluateFinishedSnapshots();

    assertThat(result.seasonsProcessed()).isEqualTo(3);
    assertThat(result.newlyEvaluatedSnapshots()).isEqualTo(6);
    assertThat(result.failedSeasons()).isZero();
  }

  private Match match(Long id) {
    Match match = mock(Match.class);
    when(match.getId()).thenReturn(id);
    return match;
  }

  private ModelEvaluationApplicationService.ModelEvaluationReport report(
      Long seasonId, Provider provider, int newlyEvaluated) {
    return new ModelEvaluationApplicationService.ModelEvaluationReport(
        seasonId,
        provider,
        "poisson-ema-player-impact-v2",
        5,
        newlyEvaluated,
        0,
        newlyEvaluated,
        null,
        null,
        List.of());
  }
}
