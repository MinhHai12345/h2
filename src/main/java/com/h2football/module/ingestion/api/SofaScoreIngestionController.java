package com.h2football.module.ingestion.api;

import com.h2football.module.ingestion.application.SofaScoreMasterDataIngestionService;
import com.h2football.module.ingestion.application.SofaScoreMatchIngestionService;
import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion/sofascore")
public class SofaScoreIngestionController {
  private final SofaScoreMasterDataIngestionService masterDataIngestion;
  private final SofaScoreMatchIngestionService matchIngestion;

  @Value("${app.ingestion.operations.api-key:}")
  private String configuredApiKey;

  public SofaScoreIngestionController(
      SofaScoreMasterDataIngestionService masterDataIngestion,
      SofaScoreMatchIngestionService matchIngestion) {
    this.masterDataIngestion = masterDataIngestion;
    this.matchIngestion = matchIngestion;
  }

  @PostMapping("/leagues/sync")
  public ResponseEntity<?> syncLeagues(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String suppliedApiKey,
      @RequestBody(required = false) LeagueSyncRequest request) {
    ResponseEntity<Map<String, String>> authorization = authorize(suppliedApiKey);
    if (authorization != null)
      return ResponseEntity.status(authorization.getStatusCode()).body(authorization.getBody());
    List<Long> ids = request == null ? List.of() : request.tournamentIds();
    return ResponseEntity.ok(masterDataIngestion.syncLeagues(ids));
  }

  @PostMapping("/teams/sync")
  public ResponseEntity<?> syncTeams(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String suppliedApiKey,
      @RequestParam long tournamentId,
      @RequestParam long seasonId) {
    ResponseEntity<Map<String, String>> authorization = authorize(suppliedApiKey);
    if (authorization != null)
      return ResponseEntity.status(authorization.getStatusCode()).body(authorization.getBody());
    return ResponseEntity.ok(masterDataIngestion.syncTeams(tournamentId, seasonId));
  }

  @PostMapping("/matches/sync")
  public ResponseEntity<?> syncMatches(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String suppliedApiKey,
      @RequestParam long tournamentId,
      @RequestParam long sofaSeasonId,
      @RequestParam long canonicalSeasonId,
      @RequestParam SofaScoreEventWindow window,
      @RequestParam(defaultValue = "0") int page) {
    ResponseEntity<Map<String, String>> authorization = authorize(suppliedApiKey);
    if (authorization != null)
      return ResponseEntity.status(authorization.getStatusCode()).body(authorization.getBody());
    return ResponseEntity.ok(
        matchIngestion.sync(tournamentId, sofaSeasonId, canonicalSeasonId, window, page));
  }

  private ResponseEntity<Map<String, String>> authorize(String suppliedApiKey) {
    if (configuredApiKey == null || configuredApiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "message",
                  "SofaScore ingestion is disabled: configure INGESTION_ADMIN_API_KEY."));
    }
    boolean valid =
        suppliedApiKey != null
            && MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                suppliedApiKey.getBytes(StandardCharsets.UTF_8));
    if (!valid)
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("message", "Invalid ingestion admin key."));
    return null;
  }

  public record LeagueSyncRequest(List<Long> tournamentIds) {
    public LeagueSyncRequest {
      tournamentIds = tournamentIds == null ? List.of() : List.copyOf(tournamentIds);
    }
  }
}
