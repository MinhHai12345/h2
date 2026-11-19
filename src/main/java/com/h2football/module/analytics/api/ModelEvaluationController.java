package com.h2football.module.analytics.api;

import com.h2football.module.analytics.application.ModelEvaluationApplicationService;
import com.h2football.module.analytics.application.ModelEvaluationApplicationService.ModelEvaluationReport;
import com.h2football.module.identity.domain.model.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seasons/{seasonId}/model-evaluation")
public class ModelEvaluationController {
  private final ModelEvaluationApplicationService evaluation;

  @Value("${app.ingestion.operations.api-key:}")
  private String configuredApiKey;

  public ModelEvaluationController(ModelEvaluationApplicationService evaluation) {
    this.evaluation = evaluation;
  }

  @GetMapping
  public List<ModelEvaluationReport> find(
      @PathVariable Long seasonId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return evaluation.findSeasonEvaluation(seasonId, provider);
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
    return ResponseEntity.ok(evaluation.evaluateSeason(seasonId, provider));
  }

  private ResponseEntity<Map<String, String>> authorize(String suppliedApiKey) {
    if (configuredApiKey == null || configuredApiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("message", "Model evaluation is disabled: API key is not configured"));
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
}
