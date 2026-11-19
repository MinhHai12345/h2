package com.h2football.module.workflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.analytics.application.MatchAnalyticsApplicationService;
import com.h2football.module.analytics.application.PlayerSeasonStatisticsApplicationService;
import com.h2football.module.analytics.application.TeamSeasonStatisticsApplicationService;
import com.h2football.module.analytics.domain.model.MatchAnalyticsStatus;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsRefreshStageHandler implements WorkflowStageHandler {
  private final ObjectMapper objectMapper;
  private final MatchRepository matches;
  private final TeamSeasonStatisticsApplicationService teamStatistics;
  private final PlayerSeasonStatisticsApplicationService playerStatistics;
  private final MatchAnalyticsApplicationService matchAnalytics;
  private final Clock clock;

  public AnalyticsRefreshStageHandler(
      ObjectMapper objectMapper,
      MatchRepository matches,
      TeamSeasonStatisticsApplicationService teamStatistics,
      PlayerSeasonStatisticsApplicationService playerStatistics,
      MatchAnalyticsApplicationService matchAnalytics,
      Clock clock) {
    this.objectMapper = objectMapper;
    this.matches = matches;
    this.teamStatistics = teamStatistics;
    this.playerStatistics = playerStatistics;
    this.matchAnalytics = matchAnalytics;
    this.clock = clock;
  }

  @Override
  public String execute(WorkflowExecutionContext context) throws Exception {
    SofaScoreSeasonWorkflowRequest request =
        objectMapper.readValue(context.requestPayload(), SofaScoreSeasonWorkflowRequest.class);
    Long seasonId = request.canonicalSeasonId();
    Provider provider = Provider.SOFASCORE;

    var teamResult = teamStatistics.rebuild(seasonId, provider);
    var playerResult = playerStatistics.rebuild(seasonId, provider);
    Instant now = Instant.now(clock);
    int refreshedMatches = 0;
    int insufficientMatches = 0;
    for (Match match : matches.findAllBySeasonIdOrderByKickoffAtAsc(seasonId)) {
      if (match.getStatus() == MatchStatus.CANCELED
          || match.getStatus() == MatchStatus.ABANDONED
          || !match.getKickoffAt().isAfter(now)) {
        continue;
      }
      var analytics = matchAnalytics.rebuild(match.getId(), provider);
      refreshedMatches++;
      if (analytics.getStatus() == MatchAnalyticsStatus.INSUFFICIENT_DATA) {
        insufficientMatches++;
      }
    }
    return objectMapper.writeValueAsString(
        new AnalyticsRefreshCheckpoint(
            seasonId,
            provider,
            teamResult.aggregatedTeams(),
            teamResult.storedStatistics(),
            playerResult.aggregatedPlayers(),
            playerResult.storedPlayers(),
            refreshedMatches,
            insufficientMatches));
  }

  public record AnalyticsRefreshCheckpoint(
      Long seasonId,
      Provider provider,
      int aggregatedTeams,
      int teamStatisticsStored,
      int aggregatedPlayers,
      int playerStatisticsStored,
      int upcomingMatchesRefreshed,
      int matchesWithInsufficientData) {}
}
