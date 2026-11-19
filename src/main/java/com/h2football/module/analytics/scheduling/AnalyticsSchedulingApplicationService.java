package com.h2football.module.analytics.scheduling;

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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsSchedulingApplicationService {
  private static final Logger log =
      LoggerFactory.getLogger(AnalyticsSchedulingApplicationService.class);

  private final MatchRepository matches;
  private final MatchAnalyticsRepository analytics;
  private final MatchPredictionSnapshotRepository predictionSnapshots;
  private final MatchAnalyticsApplicationService matchAnalytics;
  private final ModelEvaluationApplicationService modelEvaluation;
  private final AnalyticsSchedulerProperties properties;
  private final Clock clock;

  public AnalyticsSchedulingApplicationService(
      MatchRepository matches,
      MatchAnalyticsRepository analytics,
      MatchPredictionSnapshotRepository predictionSnapshots,
      MatchAnalyticsApplicationService matchAnalytics,
      ModelEvaluationApplicationService modelEvaluation,
      AnalyticsSchedulerProperties properties,
      Clock clock) {
    this.matches = matches;
    this.analytics = analytics;
    this.predictionSnapshots = predictionSnapshots;
    this.matchAnalytics = matchAnalytics;
    this.modelEvaluation = modelEvaluation;
    this.properties = properties;
    this.clock = clock;
  }

  public RefreshRunSummary refreshUpcomingMatches() {
    Instant now = Instant.now(clock);
    Instant horizon = now.plusSeconds(properties.getLookaheadHours() * 3600L);
    Instant refreshThreshold = now.minusSeconds(properties.getRefreshAfterMinutes() * 60L);
    List<Match> upcoming =
        matches.findAllByStatusAndKickoffAtBetweenOrderByKickoffAtAsc(
            MatchStatus.NOT_STARTED, now, horizon);

    int providerRuns = 0;
    int rebuilt = 0;
    int skippedFresh = 0;
    int failures = 0;

    for (Provider provider : configuredProviders()) {
      for (Match match : upcoming) {
        providerRuns++;
        try {
          MatchAnalytics current =
              analytics.findByMatchIdAndProvider(match.getId(), provider).orElse(null);
          if (current != null
              && current.getComputedAt() != null
              && !current.getComputedAt().isBefore(refreshThreshold)) {
            skippedFresh++;
            continue;
          }
          matchAnalytics.rebuild(match.getId(), provider);
          rebuilt++;
        } catch (RuntimeException exception) {
          failures++;
          log.warn(
              "Unable to refresh match analytics for matchId={} provider={}",
              match.getId(),
              provider,
              exception);
        }
      }
    }

    RefreshRunSummary summary =
        new RefreshRunSummary(upcoming.size(), providerRuns, rebuilt, skippedFresh, failures);
    log.info(
        "Match analytics refresh completed: candidates={}, providerRuns={}, rebuilt={}, "
            + "skippedFresh={}, failures={}",
        summary.candidateMatches(),
        summary.providerRuns(),
        summary.rebuiltMatches(),
        summary.skippedFreshMatches(),
        summary.failedRuns());
    return summary;
  }

  public EvaluationRunSummary evaluateFinishedSnapshots() {
    int seasonsProcessed = 0;
    int newlyEvaluated = 0;
    int failures = 0;

    for (Provider provider : configuredProviders()) {
      List<Long> seasonIds =
          predictionSnapshots.findDistinctSeasonIdsByProviderAndMatchStatus(
              provider, MatchStatus.FINISHED);
      for (Long seasonId : seasonIds) {
        try {
          List<ModelEvaluationApplicationService.ModelEvaluationReport> reports =
              modelEvaluation.evaluateSeason(seasonId, provider);
          newlyEvaluated +=
              reports.stream()
                  .mapToInt(
                      ModelEvaluationApplicationService.ModelEvaluationReport
                          ::newlyEvaluatedSnapshots)
                  .sum();
          seasonsProcessed++;
        } catch (RuntimeException exception) {
          failures++;
          log.warn(
              "Unable to evaluate model snapshots for seasonId={} provider={}",
              seasonId,
              provider,
              exception);
        }
      }
    }

    EvaluationRunSummary summary =
        new EvaluationRunSummary(seasonsProcessed, newlyEvaluated, failures);
    log.info(
        "Model evaluation completed: seasonsProcessed={}, newlyEvaluatedSnapshots={}, failures={}",
        summary.seasonsProcessed(),
        summary.newlyEvaluatedSnapshots(),
        summary.failedSeasons());
    return summary;
  }

  private List<Provider> configuredProviders() {
    return properties.getProviders().stream().distinct().toList();
  }

  public record RefreshRunSummary(
      int candidateMatches,
      int providerRuns,
      int rebuiltMatches,
      int skippedFreshMatches,
      int failedRuns) {}

  public record EvaluationRunSummary(
      int seasonsProcessed, int newlyEvaluatedSnapshots, int failedSeasons) {}
}
