package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2football.module.analytics.domain.model.MatchPredictionSnapshot;
import com.h2football.module.analytics.domain.repository.MatchPredictionSnapshotRepository;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelEvaluationApplicationServiceTest {
  private MatchPredictionSnapshotRepository snapshots;
  private ModelEvaluationApplicationService service;
  private Match match;

  @BeforeEach
  void setUp() {
    snapshots = mock(MatchPredictionSnapshotRepository.class);
    service =
        new ModelEvaluationApplicationService(
            snapshots,
            new ModelEvaluationCalculator(),
            Clock.fixed(Instant.parse("2026-01-10T00:00:00Z"), ZoneOffset.UTC));
    match = mock(Match.class);
    when(match.getId()).thenReturn(10L);
    when(match.getKickoffAt()).thenReturn(Instant.parse("2026-01-05T12:00:00Z"));
    when(match.getStatus()).thenReturn(MatchStatus.FINISHED);
    when(match.getHomeScore()).thenReturn(2);
    when(match.getAwayScore()).thenReturn(1);
  }

  @Test
  void evaluatesFinishedMatchAndBuildsCalibrationBucket() {
    MatchPredictionSnapshot snapshot = snapshot();
    when(snapshots.findAllByMatchSeasonIdAndProviderOrderByMatchKickoffAtAscComputedAtAsc(
            5L, Provider.SOFASCORE))
        .thenReturn(List.of(snapshot));

    var report = service.evaluateSeason(5L, Provider.SOFASCORE).get(0);

    assertThat(report.evaluatedSnapshots()).isEqualTo(1);
    assertThat(report.newlyEvaluatedSnapshots()).isEqualTo(1);
    assertThat(report.averageBrierScore()).isEqualByComparingTo("0.1400000000");
    assertThat(report.averageLogLoss()).isEqualByComparingTo("0.35667494");
    assertThat(report.calibration()).hasSize(1);
    assertThat(report.calibration().get(0).bucketIndex()).isEqualTo(7);
    assertThat(report.calibration().get(0).observedAccuracy()).isEqualByComparingTo("1.00000000");
    assertThat(snapshot.getActualOutcome()).isEqualTo("HOME");
  }

  @Test
  void leavesUnfinishedMatchesPending() {
    when(match.getStatus()).thenReturn(MatchStatus.NOT_STARTED);
    MatchPredictionSnapshot snapshot = snapshot();
    when(snapshots.findAllByMatchSeasonIdAndProviderOrderByMatchKickoffAtAscComputedAtAsc(
            5L, Provider.SOFASCORE))
        .thenReturn(List.of(snapshot));

    var report = service.evaluateSeason(5L, Provider.SOFASCORE).get(0);

    assertThat(report.pendingSnapshots()).isEqualTo(1);
    assertThat(report.evaluatedSnapshots()).isZero();
    assertThat(snapshot.isEvaluated()).isFalse();
  }

  @Test
  void doesNotReevaluateAnUnchangedFinalResult() {
    MatchPredictionSnapshot snapshot = snapshot();
    snapshot.evaluate(
        2,
        1,
        new BigDecimal("0.14"),
        new BigDecimal("0.3566749439"),
        Instant.parse("2026-01-06T00:00:00Z"));
    when(snapshots.findAllByMatchSeasonIdAndProviderOrderByMatchKickoffAtAscComputedAtAsc(
            5L, Provider.SOFASCORE))
        .thenReturn(List.of(snapshot));

    var report = service.evaluateSeason(5L, Provider.SOFASCORE).get(0);

    assertThat(report.newlyEvaluatedSnapshots()).isZero();
    assertThat(snapshot.getEvaluatedAt()).isEqualTo(Instant.parse("2026-01-06T00:00:00Z"));
  }

  private MatchPredictionSnapshot snapshot() {
    return new MatchPredictionSnapshot(
        match,
        Provider.SOFASCORE,
        "poisson-ema-v1",
        Instant.parse("2026-01-05T10:00:00Z"),
        new BigDecimal("0.70"),
        new BigDecimal("0.20"),
        new BigDecimal("0.10"));
  }
}
