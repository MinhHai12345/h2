package com.h2football.module.analytics.api;

import com.h2football.module.analytics.application.PlayerSeasonStatisticsApplicationService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
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
@RequestMapping("/api/v1/seasons/{seasonId}/player-statistics")
public class PlayerSeasonStatisticsController {
  private final PlayerSeasonStatisticsApplicationService statistics;

  @Value("${app.ingestion.operations.api-key:}")
  private String configuredApiKey;

  public PlayerSeasonStatisticsController(PlayerSeasonStatisticsApplicationService statistics) {
    this.statistics = statistics;
  }

  @GetMapping
  public List<PlayerSeasonStatisticResponse> find(
      @PathVariable Long seasonId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return statistics.findBySeason(seasonId, provider).stream()
        .map(PlayerSeasonStatisticResponse::from)
        .toList();
  }

  @PostMapping("/rebuild")
  public ResponseEntity<?> rebuild(
      @PathVariable Long seasonId,
      @RequestParam(defaultValue = "SOFASCORE") Provider provider,
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String suppliedApiKey) {
    ResponseEntity<Map<String, String>> authorization = authorize(suppliedApiKey);
    if (authorization != null) {
      return ResponseEntity.status(authorization.getStatusCode()).body(authorization.getBody());
    }
    return ResponseEntity.ok(statistics.rebuild(seasonId, provider));
  }

  private ResponseEntity<Map<String, String>> authorize(String suppliedApiKey) {
    if (configuredApiKey == null || configuredApiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "message",
                  "Statistics rebuild is disabled because its API key is not configured"));
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

  public record PlayerSeasonStatisticResponse(
      Long playerId,
      String playerName,
      Provider provider,
      Long appearances,
      Long minutesPlayed,
      Long goals,
      Long assists,
      Long totalShots,
      Long shotsOnTarget,
      Long accuratePasses,
      Long totalPasses,
      Long keyPasses,
      Long tackles,
      Long interceptions,
      Long clearances,
      BigDecimal expectedGoals,
      BigDecimal expectedAssists,
      BigDecimal averageRating) {
    static PlayerSeasonStatisticResponse from(PlayerSeasonStatistic item) {
      return new PlayerSeasonStatisticResponse(
          item.getPlayer().getId(),
          item.getPlayer().getName(),
          item.getProvider(),
          item.getAppearances(),
          item.getMinutesPlayed(),
          item.getGoals(),
          item.getAssists(),
          item.getTotalShots(),
          item.getShotsOnTarget(),
          item.getAccuratePasses(),
          item.getTotalPasses(),
          item.getKeyPasses(),
          item.getTackles(),
          item.getInterceptions(),
          item.getClearances(),
          item.getExpectedGoals(),
          item.getExpectedAssists(),
          item.getAverageRating());
    }
  }
}
