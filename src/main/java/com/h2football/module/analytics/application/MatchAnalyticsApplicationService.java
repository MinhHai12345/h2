package com.h2football.module.analytics.application;

import com.h2football.module.analytics.application.MatchAnalyticsCalculator.MatchProbabilities;
import com.h2football.module.analytics.application.PlayerImpactApplicationService.MatchPlayerImpactResponse;
import com.h2football.module.analytics.application.PlayerImpactApplicationService.TeamImpact;
import com.h2football.module.analytics.domain.model.MatchAnalytics;
import com.h2football.module.analytics.domain.model.MatchAnalyticsStatus;
import com.h2football.module.analytics.domain.repository.MatchAnalyticsRepository;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.application.MatchNotFoundException;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatistic;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchAnalyticsApplicationService {
  private static final int FORM_PERIOD = 5;
  private static final int XG_HISTORY_LIMIT = 10;
  private static final String EXPECTED_GOALS_KEY = "expectedGoals";
  private static final String ALL_PERIOD = "ALL";
  private static final String MODEL_VERSION = "poisson-ema-player-impact-v2";

  private final MatchRepository matches;
  private final MatchStatisticRepository matchStatistics;
  private final MatchAnalyticsRepository analytics;
  private final MatchAnalyticsCalculator calculator;
  private final PlayerImpactApplicationService playerImpact;
  private final ModelEvaluationApplicationService modelEvaluation;
  private final Clock clock;

  public MatchAnalyticsApplicationService(
      MatchRepository matches,
      MatchStatisticRepository matchStatistics,
      MatchAnalyticsRepository analytics,
      MatchAnalyticsCalculator calculator,
      PlayerImpactApplicationService playerImpact,
      ModelEvaluationApplicationService modelEvaluation,
      Clock clock) {
    this.matches = matches;
    this.matchStatistics = matchStatistics;
    this.analytics = analytics;
    this.calculator = calculator;
    this.playerImpact = playerImpact;
    this.modelEvaluation = modelEvaluation;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public MatchAnalytics findByMatch(Long matchId, Provider provider) {
    validate(matchId, provider);
    return analytics
        .findByMatchIdAndProvider(matchId, provider)
        .orElseThrow(() -> new MatchAnalyticsNotFoundException(matchId, provider));
  }

  @Transactional(readOnly = true)
  public List<MatchAnalytics> findBySeason(Long seasonId, Provider provider) {
    if (seasonId == null || seasonId <= 0 || provider == null) {
      throw new IllegalArgumentException("Positive seasonId and provider are required");
    }
    return analytics.findAllByMatchSeasonIdAndProviderOrderByMatchKickoffAtAsc(seasonId, provider);
  }

  public MatchAnalytics rebuild(Long matchId, Provider provider) {
    validate(matchId, provider);
    Match target = matches.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    List<Match> history =
        matches.findAllBySeasonIdOrderByKickoffAtAsc(target.getSeason().getId()).stream()
            .filter(item -> item.getStatus() == MatchStatus.FINISHED)
            .filter(item -> item.getHomeScore() != null && item.getAwayScore() != null)
            .filter(item -> item.getKickoffAt().isBefore(target.getKickoffAt()))
            .filter(
                item ->
                    item.getHomeTeam().getId().equals(target.getHomeTeam().getId())
                        || item.getAwayTeam().getId().equals(target.getHomeTeam().getId())
                        || item.getHomeTeam().getId().equals(target.getAwayTeam().getId())
                        || item.getAwayTeam().getId().equals(target.getAwayTeam().getId()))
            .sorted(Comparator.comparing(Match::getKickoffAt).reversed())
            .toList();

    Map<Long, MatchStatistic> expectedGoalsByMatch = loadExpectedGoals(history, provider);
    TeamHistory homeHistory =
        buildHistory(history, target.getHomeTeam().getId(), expectedGoalsByMatch);
    TeamHistory awayHistory =
        buildHistory(history, target.getAwayTeam().getId(), expectedGoalsByMatch);
    BigDecimal homeForm = calculator.calculateEmaForm(homeHistory.points(), FORM_PERIOD);
    BigDecimal awayForm = calculator.calculateEmaForm(awayHistory.points(), FORM_PERIOD);
    BigDecimal homeXgFor = average(homeHistory.expectedGoalsFor());
    BigDecimal homeXgAgainst = average(homeHistory.expectedGoalsAgainst());
    BigDecimal awayXgFor = average(awayHistory.expectedGoalsFor());
    BigDecimal awayXgAgainst = average(awayHistory.expectedGoalsAgainst());

    MatchAnalyticsSnapshot snapshot;
    if (homeXgFor == null || awayXgFor == null || homeXgAgainst == null || awayXgAgainst == null) {
      snapshot =
          insufficient(
              homeForm,
              awayForm,
              homeHistory.matchesPlayed(),
              awayHistory.matchesPlayed(),
              missingMetrics(homeXgFor, awayXgFor, homeXgAgainst, awayXgAgainst));
    } else {
      BigDecimal baseHome =
          homeXgFor.add(awayXgAgainst).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
      BigDecimal baseAway =
          awayXgFor.add(homeXgAgainst).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
      BigDecimal homeLambda = calculator.adjustExpectedGoals(baseHome, homeForm, awayForm);
      BigDecimal awayLambda = calculator.adjustExpectedGoals(baseAway, awayForm, homeForm);

      MatchPlayerImpactResponse impact = playerImpact.calculate(target.getId(), provider);
      BigDecimal combinedCoverage = combinedCoverage(impact);
      TeamImpact homeImpact = impact == null ? null : impact.home();
      TeamImpact awayImpact = impact == null ? null : impact.away();
      homeLambda =
          calculator.adjustExpectedGoalsByPlayerImpact(
              homeLambda,
              homeImpact == null ? null : homeImpact.averageAttackingIndex(),
              awayImpact == null ? null : awayImpact.averageDefensiveIndex(),
              combinedCoverage);
      awayLambda =
          calculator.adjustExpectedGoalsByPlayerImpact(
              awayLambda,
              awayImpact == null ? null : awayImpact.averageAttackingIndex(),
              homeImpact == null ? null : homeImpact.averageDefensiveIndex(),
              combinedCoverage);

      MatchProbabilities probabilities = calculator.calculateProbabilities(homeLambda, awayLambda);
      snapshot =
          new MatchAnalyticsSnapshot(
              MatchAnalyticsStatus.READY,
              homeLambda,
              awayLambda,
              homeForm,
              awayForm,
              probabilities.homeWin(),
              probabilities.draw(),
              probabilities.awayWin(),
              probabilities.over25(),
              probabilities.bothTeamsScore(),
              calculator.calculateMatchAttractivenessIndex(homeLambda, awayLambda),
              homeHistory.matchesPlayed(),
              awayHistory.matchesPlayed(),
              null);
    }
    return persist(target, provider, snapshot);
  }

  private BigDecimal combinedCoverage(MatchPlayerImpactResponse impact) {
    if (impact == null
        || impact.home() == null
        || impact.away() == null
        || !impact.home().lineupConfirmed()
        || !impact.away().lineupConfirmed()
        || impact.home().dataCoveragePercent() == null
        || impact.away().dataCoveragePercent() == null) {
      return BigDecimal.ZERO;
    }
    return impact.home().dataCoveragePercent().min(impact.away().dataCoveragePercent());
  }

  private Map<Long, MatchStatistic> loadExpectedGoals(List<Match> history, Provider provider) {
    List<Long> matchIds = history.stream().map(Match::getId).toList();
    if (matchIds.isEmpty()) return Map.of();
    return matchStatistics
        .findAllByMatchIdInAndProviderOrderByMatchIdAscPeriodAscGroupNameAscStatisticKeyAsc(
            matchIds, provider)
        .stream()
        .filter(item -> ALL_PERIOD.equalsIgnoreCase(item.getPeriod()))
        .filter(item -> EXPECTED_GOALS_KEY.equalsIgnoreCase(item.getStatisticKey()))
        .collect(
            Collectors.toMap(
                item -> item.getMatch().getId(), item -> item, (first, second) -> first));
  }

  private TeamHistory buildHistory(
      List<Match> history, Long teamId, Map<Long, MatchStatistic> expectedGoalsByMatch) {
    List<Integer> points = new ArrayList<>();
    List<BigDecimal> expectedGoalsFor = new ArrayList<>();
    List<BigDecimal> expectedGoalsAgainst = new ArrayList<>();
    int matchesPlayed = 0;
    for (Match match : history) {
      boolean home = match.getHomeTeam().getId().equals(teamId);
      boolean away = match.getAwayTeam().getId().equals(teamId);
      if (!home && !away) continue;
      matchesPlayed++;
      int scored = home ? match.getHomeScore() : match.getAwayScore();
      int conceded = home ? match.getAwayScore() : match.getHomeScore();
      points.add(scored > conceded ? 3 : scored == conceded ? 1 : 0);

      MatchStatistic statistic = expectedGoalsByMatch.get(match.getId());
      if (statistic == null) continue;
      BigDecimal forValue = home ? statistic.getHomeValue() : statistic.getAwayValue();
      BigDecimal againstValue = home ? statistic.getAwayValue() : statistic.getHomeValue();
      if (forValue != null && expectedGoalsFor.size() < XG_HISTORY_LIMIT) {
        expectedGoalsFor.add(forValue);
      }
      if (againstValue != null && expectedGoalsAgainst.size() < XG_HISTORY_LIMIT) {
        expectedGoalsAgainst.add(againstValue);
      }
    }
    return new TeamHistory(
        List.copyOf(points.subList(0, Math.min(points.size(), FORM_PERIOD))),
        List.copyOf(expectedGoalsFor),
        List.copyOf(expectedGoalsAgainst),
        matchesPlayed);
  }

  private MatchAnalyticsSnapshot insufficient(
      BigDecimal homeForm, BigDecimal awayForm, int homeMatches, int awayMatches, String reason) {
    return new MatchAnalyticsSnapshot(
        MatchAnalyticsStatus.INSUFFICIENT_DATA,
        null,
        null,
        homeForm,
        awayForm,
        null,
        null,
        null,
        null,
        null,
        null,
        homeMatches,
        awayMatches,
        reason);
  }

  private String missingMetrics(
      BigDecimal homeFor, BigDecimal awayFor, BigDecimal homeAgainst, BigDecimal awayAgainst) {
    List<String> missing = new ArrayList<>();
    if (homeFor == null) missing.add("home expected goals for");
    if (awayFor == null) missing.add("away expected goals for");
    if (homeAgainst == null) missing.add("home expected goals against");
    if (awayAgainst == null) missing.add("away expected goals against");
    return "Missing historical expectedGoals observations: " + String.join(", ", missing);
  }

  private MatchAnalytics persist(Match match, Provider provider, MatchAnalyticsSnapshot snapshot) {
    Instant now = Instant.now(clock);
    MatchAnalytics entity =
        analytics
            .findByMatchIdAndProvider(match.getId(), provider)
            .orElseGet(
                () ->
                    new MatchAnalytics(
                        match,
                        provider,
                        snapshot.status(),
                        snapshot.homeExpectedGoals(),
                        snapshot.awayExpectedGoals(),
                        snapshot.homeFormIndex(),
                        snapshot.awayFormIndex(),
                        snapshot.homeWinProbability(),
                        snapshot.drawProbability(),
                        snapshot.awayWinProbability(),
                        snapshot.over25Probability(),
                        snapshot.bothTeamsScoreProbability(),
                        snapshot.matchAttractivenessIndex(),
                        snapshot.homeHistoryMatches(),
                        snapshot.awayHistoryMatches(),
                        MODEL_VERSION,
                        snapshot.reason(),
                        now));
    entity.updateSnapshot(
        snapshot.status(),
        snapshot.homeExpectedGoals(),
        snapshot.awayExpectedGoals(),
        snapshot.homeFormIndex(),
        snapshot.awayFormIndex(),
        snapshot.homeWinProbability(),
        snapshot.drawProbability(),
        snapshot.awayWinProbability(),
        snapshot.over25Probability(),
        snapshot.bothTeamsScoreProbability(),
        snapshot.matchAttractivenessIndex(),
        snapshot.homeHistoryMatches(),
        snapshot.awayHistoryMatches(),
        MODEL_VERSION,
        snapshot.reason(),
        now);
    MatchAnalytics saved = analytics.save(entity);
    modelEvaluation.recordPreMatchSnapshot(saved);
    return saved;
  }

  private BigDecimal average(List<BigDecimal> values) {
    return values.isEmpty()
        ? null
        : values.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
  }

  private void validate(Long matchId, Provider provider) {
    if (matchId == null || matchId <= 0 || provider == null) {
      throw new IllegalArgumentException("Positive matchId and provider are required");
    }
  }

  private record TeamHistory(
      List<Integer> points,
      List<BigDecimal> expectedGoalsFor,
      List<BigDecimal> expectedGoalsAgainst,
      int matchesPlayed) {}

  private record MatchAnalyticsSnapshot(
      MatchAnalyticsStatus status,
      BigDecimal homeExpectedGoals,
      BigDecimal awayExpectedGoals,
      BigDecimal homeFormIndex,
      BigDecimal awayFormIndex,
      BigDecimal homeWinProbability,
      BigDecimal drawProbability,
      BigDecimal awayWinProbability,
      BigDecimal over25Probability,
      BigDecimal bothTeamsScoreProbability,
      BigDecimal matchAttractivenessIndex,
      int homeHistoryMatches,
      int awayHistoryMatches,
      String reason) {}
}
