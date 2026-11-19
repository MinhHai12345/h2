package com.h2football.module.operations.api;

import com.h2football.module.operations.api.CronJobDtos.CronJobResponse;
import com.h2football.module.operations.api.CronJobDtos.TriggerRequest;
import com.h2football.module.operations.application.CronJobApplicationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/cron-jobs")
public class CronJobController {
  private final CronJobApplicationService cronJobs;

  @Value("${app.ingestion.operations.api-key:}")
  private String configuredApiKey;

  public CronJobController(CronJobApplicationService cronJobs) {
    this.cronJobs = cronJobs;
  }

  @GetMapping("/metadata")
  public ResponseEntity<?> metadata(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey) {
    ResponseEntity<?> denied = authorize(apiKey);
    if (denied != null) return denied;
    var metadata = cronJobs.getSchedulerMetadata();
    return ResponseEntity.ok(
        new SchedulerMetadataResponse(
            metadata.getSchedulerName(),
            metadata.getSchedulerInstanceId(),
            metadata.isStarted(),
            metadata.isInStandbyMode(),
            metadata.isShutdown(),
            metadata.getNumberOfJobsExecuted()));
  }

  @GetMapping
  public ResponseEntity<?> findAll(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey) {
    ResponseEntity<?> denied = authorize(apiKey);
    return denied == null
        ? ResponseEntity.ok(cronJobs.findAll().stream().map(CronJobResponse::from).toList())
        : denied;
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey,
      @PathVariable Long id) {
    ResponseEntity<?> denied = authorize(apiKey);
    return denied == null ? ResponseEntity.ok(CronJobResponse.from(cronJobs.get(id))) : denied;
  }

  @PostMapping("/{id}/pause")
  public ResponseEntity<?> pause(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey,
      @PathVariable Long id) {
    ResponseEntity<?> denied = authorize(apiKey);
    return denied == null ? ResponseEntity.ok(CronJobResponse.from(cronJobs.pause(id))) : denied;
  }

  @PostMapping("/{id}/resume")
  public ResponseEntity<?> resume(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey,
      @PathVariable Long id) {
    ResponseEntity<?> denied = authorize(apiKey);
    return denied == null ? ResponseEntity.ok(CronJobResponse.from(cronJobs.resume(id))) : denied;
  }

  @PostMapping("/{id}/stop")
  public ResponseEntity<?> stop(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey,
      @PathVariable Long id) {
    ResponseEntity<?> denied = authorize(apiKey);
    return denied == null ? ResponseEntity.ok(CronJobResponse.from(cronJobs.stop(id))) : denied;
  }

  @PostMapping("/{id}/start")
  public ResponseEntity<?> start(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey,
      @PathVariable Long id,
      @RequestBody(required = false) TriggerRequest request) {
    ResponseEntity<?> denied = authorize(apiKey);
    if (denied != null) return denied;
    java.util.Map<String, Long> parameters = new java.util.LinkedHashMap<>();
    if (request != null) {
      if (request.tournamentId() != null) parameters.put("tournamentId", request.tournamentId());
      if (request.sofaSeasonId() != null) parameters.put("sofaSeasonId", request.sofaSeasonId());
      if (request.canonicalSeasonId() != null)
        parameters.put("canonicalSeasonId", request.canonicalSeasonId());
    }
    return ResponseEntity.ok(CronJobResponse.from(cronJobs.triggerNow(id, parameters)));
  }

  @PatchMapping("/{id}/schedule")
  public ResponseEntity<?> updateSchedule(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey,
      @PathVariable Long id,
      @RequestBody ScheduleRequest request) {
    ResponseEntity<?> denied = authorize(apiKey);
    return denied == null
        ? ResponseEntity.ok(
            CronJobResponse.from(cronJobs.updateSchedule(id, request.cronExpression())))
        : denied;
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(
      @RequestHeader(name = "X-Ingestion-Admin-Key", required = false) String apiKey,
      @PathVariable Long id) {
    ResponseEntity<?> denied = authorize(apiKey);
    if (denied != null) return denied;
    cronJobs.delete(id);
    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<?> authorize(String suppliedApiKey) {
    if (configuredApiKey == null || configuredApiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "message",
                  "Cron job management is disabled: configure INGESTION_ADMIN_API_KEY."));
    }
    boolean valid =
        suppliedApiKey != null
            && MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                suppliedApiKey.getBytes(StandardCharsets.UTF_8));
    return valid
        ? null
        : ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("message", "Invalid ingestion admin key."));
  }

  public record ScheduleRequest(String cronExpression) {}

  public record SchedulerMetadataResponse(
      String schedulerName,
      String instanceId,
      boolean started,
      boolean inStandbyMode,
      boolean shutdown,
      int executedJobCount) {}
}
