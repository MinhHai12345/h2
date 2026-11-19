package com.h2football.module.analytics.application;

import com.h2football.module.analytics.application.ModelEvaluationCalculator.EvaluationMetrics;
import com.h2football.module.analytics.domain.model.MatchAnalytics;
import com.h2football.module.analytics.domain.model.MatchAnalyticsStatus;
import com.h2football.module.analytics.domain.model.MatchPredictionSnapshot;
import com.h2football.module.analytics.domain.repository.MatchPredictionSnapshotRepository;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ModelEvaluationApplicationService {
  private static final int CALIBRATION_BUCKET_COUNT = 10;
  private static final int SCALE = 8;

  private final MatchPredictionSnapshotRepository snapshots;
  private final ModelEvaluationCalculator calculator;
  private final Clock clock;

  public ModelEvaluationApplicationService(
      MatchPredictionSnapshotRepository snapshots,
      ModelEvaluationCalculator calculator,
      Clock clock) {
    this.snapshots = snapshots;
    this.calculator = calculator;
    this.clock = clock;
  }

  public void recordPreMatchSnapshot(MatchAnalytics analytics) {
    if (analytics == null || analytics.getStatus() != MatchAnalyticsStatus.READY) {
      return;
    }
    Match match = analytics.getMatch();
    if (match.getKickoffAt() == null || !analytics.getComputedAt().isBefore(match.getKickoffAt())) {
      return;
    }
    if (snapshots.existsByMatchIdAndProviderAndModelVersionAndComputedAt(
        match.getId(),
        analytics.getProvider(),
        analytics.getModelVersion(),
        analytics.getComputedAt())) {
      return;
    }
    snapshots.save(
        new MatchPredictionSnapshot(
            match,
            analytics.getProvider(),
            analytics.getModelVersion(),
            analytics.getComputedAt(),
            analytics.getHomeWinProbability(),
            analytics.getDrawProbability(),
            analytics.getAwayWinProbability()));
  }

  public List<ModelEvaluationReport> evaluateSeason(Long seasonId, Provider provider) {
    validate(seasonId, provider);
    List<MatchPredictionSnapshot> source = loadSnapshots(seasonId, provider);
    Instant evaluatedAt = Instant.now(clock);
    Map<String, Integer> newlyEvaluatedByVersion = new HashMap<>();

    for (MatchPredictionSnapshot snapshot : source) {
      Match match = snapshot.getMatch();
      if (match.getStatus() != MatchStatus.FINISHED
          || match.getHomeScore() == null
          || match.getAwayScore() == null
          || snapshot.hasResult(match.getHomeScore(), match.getAwayScore())) {
        continue;
      }
      EvaluationMetrics metrics =
          calculator.calculate(
              snapshot.getHomeWinProbability(),
              snapshot.getDrawProbability(),
              snapshot.getAwayWinProbability(),
              match.getHomeScore(),
              match.getAwayScore());
      snapshot.evaluate(
          match.getHomeScore(),
          match.getAwayScore(),
          metrics.brierScore(),
          metrics.logLoss(),
          evaluatedAt);
      newlyEvaluatedByVersion.merge(snapshot.getModelVersion(), 1, Integer::sum);
    }
    return summarize(seasonId, provider, source, newlyEvaluatedByVersion);
  }

  @Transactional(readOnly = true)
  public List<ModelEvaluationReport> findSeasonEvaluation(Long seasonId, Provider provider) {
    validate(seasonId, provider);
    return summarize(seasonId, provider, loadSnapshots(seasonId, provider), Map.of());
  }

  private List<MatchPredictionSnapshot> loadSnapshots(Long seasonId, Provider provider) {
    return snapshots.findAllByMatchSeasonIdAndProviderOrderByMatchKickoffAtAscComputedAtAsc(
        seasonId, provider);
  }

  private List<ModelEvaluationReport> summarize(
      Long seasonId,
      Provider provider,
      List<MatchPredictionSnapshot> source,
      Map<String, Integer> newlyEvaluatedByVersion) {
    Map<String, List<MatchPredictionSnapshot>> byVersion =
        source.stream()
            .collect(
                Collectors.groupingBy(
                    MatchPredictionSnapshot::getModelVersion,
                    java.util.TreeMap::new,
                    Collectors.toList()));

    return byVersion.entrySet().stream()
        .map(
            entry -> {
              List<MatchPredictionSnapshot> modelSnapshots = entry.getValue();
              List<MatchPredictionSnapshot> evaluated =
                  modelSnapshots.stream().filter(MatchPredictionSnapshot::isEvaluated).toList();
              int pending =
                  (int) modelSnapshots.stream().filter(snapshot -> !snapshot.isEvaluated()).count();
              return new ModelEvaluationReport(
                  seasonId,
                  provider,
                  entry.getKey(),
                  modelSnapshots.size(),
                  evaluated.size(),
                  pending,
                  newlyEvaluatedByVersion.getOrDefault(entry.getKey(), 0),
                  average(evaluated.stream().map(MatchPredictionSnapshot::getBrierScore).toList()),
                  average(evaluated.stream().map(MatchPredictionSnapshot::getLogLoss).toList()),
                  calibration(evaluated));
            })
        .sorted(Comparator.comparing(ModelEvaluationReport::modelVersion))
        .toList();
  }

  private List<CalibrationBucket> calibration(List<MatchPredictionSnapshot> evaluated) {
    List<CalibrationBucket> result = new ArrayList<>();
    for (int bucketIndex = 0; bucketIndex < CALIBRATION_BUCKET_COUNT; bucketIndex++) {
      final int selectedBucket = bucketIndex;
      List<MatchPredictionSnapshot> bucket =
          evaluated.stream()
              .filter(
                  snapshot ->
                      bucketIndex(
                              maxProbability(
                                  snapshot.getHomeWinProbability(),
                                  snapshot.getDrawProbability(),
                                  snapshot.getAwayWinProbability()))
                          == selectedBucket)
              .toList();
      if (bucket.isEmpty()) {
        continue;
      }

      BigDecimal confidenceTotal = BigDecimal.ZERO;
      int correctPredictions = 0;
      for (MatchPredictionSnapshot snapshot : bucket) {
        BigDecimal home = snapshot.getHomeWinProbability();
        BigDecimal draw = snapshot.getDrawProbability();
        BigDecimal away = snapshot.getAwayWinProbability();
        confidenceTotal = confidenceTotal.add(maxProbability(home, draw, away));
        if (predictedOutcome(home, draw, away).equals(snapshot.getActualOutcome())) {
          correctPredictions++;
        }
      }
      result.add(
          new CalibrationBucket(
              selectedBucket,
              confidenceTotal.divide(
                  BigDecimal.valueOf(bucket.size()), SCALE, RoundingMode.HALF_UP),
              BigDecimal.valueOf(correctPredictions)
                  .divide(BigDecimal.valueOf(bucket.size()), SCALE, RoundingMode.HALF_UP),
              bucket.size()));
    }
    return List.copyOf(result);
  }

  private int bucketIndex(BigDecimal confidence) {
    return Math.min(confidence.multiply(BigDecimal.TEN).intValue(), CALIBRATION_BUCKET_COUNT - 1);
  }

  private BigDecimal maxProbability(BigDecimal home, BigDecimal draw, BigDecimal away) {
    return home.max(draw).max(away);
  }

  private String predictedOutcome(BigDecimal home, BigDecimal draw, BigDecimal away) {
    if (home.compareTo(draw) >= 0 && home.compareTo(away) >= 0) {
      return "HOME";
    }
    return draw.compareTo(away) >= 0 ? "DRAW" : "AWAY";
  }

  private BigDecimal average(List<BigDecimal> values) {
    return values.isEmpty()
        ? null
        : values.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_UP);
  }

  private void validate(Long seasonId, Provider provider) {
    if (seasonId == null || seasonId <= 0 || provider == null) {
      throw new IllegalArgumentException("Positive seasonId and provider are required");
    }
  }

  public record ModelEvaluationReport(
      Long seasonId,
      Provider provider,
      String modelVersion,
      int predictionSnapshots,
      int evaluatedSnapshots,
      int pendingSnapshots,
      int newlyEvaluatedSnapshots,
      BigDecimal averageBrierScore,
      BigDecimal averageLogLoss,
      List<CalibrationBucket> calibration) {}

  public record CalibrationBucket(
      int bucketIndex,
      BigDecimal averageConfidence,
      BigDecimal observedAccuracy,
      int sampleCount) {}
}
