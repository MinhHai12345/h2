package com.h2football.module.analytics.api;

import com.h2football.module.analytics.application.TeamMatchStatisticsApplicationService;
import com.h2football.module.analytics.application.TeamMatchStatisticsApplicationService.TeamMatchStatisticResponse;
import com.h2football.module.analytics.application.TeamSeasonStatisticsApplicationService;
import com.h2football.module.analytics.application.TeamSeasonStatisticsApplicationService.RebuildResult;
import com.h2football.module.analytics.domain.model.TeamSeasonStatistic;
import com.h2football.module.identity.domain.model.Provider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeamStatisticsController {
  private final TeamMatchStatisticsApplicationService matchStatistics;
  private final TeamSeasonStatisticsApplicationService seasonStatistics;

  @Value("${app.ingestion.operations.api-key:}")
  private String configuredApiKey;

  public TeamStatisticsController(
      TeamMatchStatisticsApplicationService matchStatistics,
      TeamSeasonStatisticsApplicationService seasonStatistics) {
    this.matchStatistics = matchStatistics;
    this.seasonStatistics = seasonStatistics;
  }

  @GetMapping("/api/v1/teams/{teamId}/matches/{matchId}/statistics")
  public List<TeamMatchStatisticResponse> findMatchStatistics(
      @PathVariable Long teamId,
      @PathVariable Long matchId,
      @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return matchStatistics.findByMatch(teamId, matchId, provider);
  }

  @GetMapping("/api/v1/seasons/{seasonId}/team-statistics")
  public List<TeamSeasonStatisticResponse> findSeasonStatistics(
      @PathVariable Long seasonId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return seasonStatistics.findBySeason(seasonId, provider).stream()
        .map(TeamSeasonStatisticResponse::from)
        .toList();
  }

  @GetMapping("/api/v1/seasons/{seasonId}/teams/{teamId}/statistics")
  public List<TeamSeasonStatisticResponse> findTeamSeasonStatistics(
      @PathVariable Long seasonId,
      @PathVariable Long teamId,
      @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return seasonStatistics.findByTeam(seasonId, teamId, provider).stream()
        .map(TeamSeasonStatisticResponse::from)
        .toList();
  }

  @PostMapping("/api/v1/seasons/{seasonId}/team-statistics/rebuild")
  public ResponseEntity<?> rebuildSeasonStatistics(
      @PathVariable Long seasonId,
      @RequestParam(defaultValue = "SOFASCORE") Provider provider,
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String suppliedApiKey) {
    ResponseEntity<Map<String, String>> authorization = authorize(suppliedApiKey);
    if (authorization != null) {
      return ResponseEntity.status(authorization.getStatusCode()).body(authorization.getBody());
    }
    RebuildResult result = seasonStatistics.rebuild(seasonId, provider);
    return ResponseEntity.ok(result);
  }

  private ResponseEntity<Map<String, String>> authorize(String suppliedApiKey) {
    if (configuredApiKey == null || configuredApiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of("message", "Team statistics rebuild is disabled: API key is not configured"));
    }
    if (suppliedApiKey == null
        || !MessageDigest.isEqual(
            configuredApiKey.getBytes(StandardCharsets.UTF_8),
            suppliedApiKey.getBytes(StandardCharsets.UTF_8))) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("message", "Invalid ingestion admin key"));
    }
    return null;
  }

  public record TeamSeasonStatisticResponse(
      Long teamId,
      String teamName,
      Provider provider,
      String period,
      String groupName,
      String statisticKey,
      String statisticName,
      Long matchesCount,
      BigDecimal totalValue,
      BigDecimal averageValue) {
    static TeamSeasonStatisticResponse from(TeamSeasonStatistic item) {
      return new TeamSeasonStatisticResponse(
          item.getTeam().getId(),
          item.getTeam().getName(),
          item.getProvider(),
          item.getPeriod(),
          item.getGroupName(),
          item.getStatisticKey(),
          item.getStatisticName(),
          item.getMatchesCount(),
          item.getTotalValue(),
          item.getAverageValue());
    }
  }
}
