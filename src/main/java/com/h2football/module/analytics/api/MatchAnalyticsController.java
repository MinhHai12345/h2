package com.h2football.module.analytics.api;

import com.h2football.module.analytics.application.MatchAnalyticsApplicationService;
import com.h2football.module.analytics.domain.model.MatchAnalytics;
import com.h2football.module.identity.domain.model.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MatchAnalyticsController {
  private final MatchAnalyticsApplicationService analytics;

  @Value("${app.ingestion.operations.api-key:}")
  private String configuredApiKey;

  public MatchAnalyticsController(MatchAnalyticsApplicationService analytics) {
    this.analytics = analytics;
  }

  @GetMapping("/api/v1/matches/{matchId}/analytics")
  public MatchAnalyticsResponse findByMatch(
      @PathVariable Long matchId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return MatchAnalyticsResponse.from(analytics.findByMatch(matchId, provider));
  }

  @GetMapping("/api/v1/seasons/{seasonId}/match-analytics")
  public List<MatchAnalyticsResponse> findBySeason(
      @PathVariable Long seasonId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return analytics.findBySeason(seasonId, provider).stream()
        .map(MatchAnalyticsResponse::from)
        .toList();
  }

  @PostMapping("/api/v1/matches/{matchId}/analytics/rebuild")
  public ResponseEntity<?> rebuild(
      @PathVariable Long matchId,
      @RequestParam(defaultValue = "SOFASCORE") Provider provider,
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String suppliedApiKey) {
    ResponseEntity<Map<String, String>> authorization = authorize(suppliedApiKey);
    if (authorization != null) {
      return ResponseEntity.status(authorization.getStatusCode()).body(authorization.getBody());
    }
    return ResponseEntity.ok(MatchAnalyticsResponse.from(analytics.rebuild(matchId, provider)));
  }

  private ResponseEntity<Map<String, String>> authorize(String suppliedApiKey) {
    if (configuredApiKey == null || configuredApiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of("message", "Match analytics rebuild is disabled: API key is not configured"));
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

  public record MatchAnalyticsResponse(
      Long matchId,
      Long seasonId,
      String homeTeam,
      String awayTeam,
      Instant kickoffAt,
      Provider provider,
      String status,
      java.math.BigDecimal homeExpectedGoals,
      java.math.BigDecimal awayExpectedGoals,
      java.math.BigDecimal homeFormIndex,
      java.math.BigDecimal awayFormIndex,
      java.math.BigDecimal homeWinProbability,
      java.math.BigDecimal drawProbability,
      java.math.BigDecimal awayWinProbability,
      java.math.BigDecimal over25Probability,
      java.math.BigDecimal bothTeamsScoreProbability,
      java.math.BigDecimal matchAttractivenessIndex,
      int homeHistoryMatches,
      int awayHistoryMatches,
      String modelVersion,
      String insufficientDataReason,
      Instant computedAt) {
    static MatchAnalyticsResponse from(MatchAnalytics item) {
      var match = item.getMatch();
      return new MatchAnalyticsResponse(
          match.getId(),
          match.getSeason().getId(),
          match.getHomeTeam().getName(),
          match.getAwayTeam().getName(),
          match.getKickoffAt(),
          item.getProvider(),
          item.getStatus().name(),
          item.getHomeExpectedGoals(),
          item.getAwayExpectedGoals(),
          item.getHomeFormIndex(),
          item.getAwayFormIndex(),
          item.getHomeWinProbability(),
          item.getDrawProbability(),
          item.getAwayWinProbability(),
          item.getOver25Probability(),
          item.getBothTeamsScoreProbability(),
          item.getMatchAttractivenessIndex(),
          item.getHomeHistoryMatches(),
          item.getAwayHistoryMatches(),
          item.getModelVersion(),
          item.getInsufficientDataReason(),
          item.getComputedAt());
    }
  }
}
