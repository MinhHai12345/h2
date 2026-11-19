package com.h2football.module.workflow.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.workflow.application.SofaScoreSeasonWorkflowRequest;
import com.h2football.module.workflow.application.WorkflowCoordinator;
import com.h2football.module.workflow.application.WorkflowRunQueryService;
import com.h2football.module.workflow.bootstrap.WorkflowConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
  private final WorkflowCoordinator coordinator;
  private final WorkflowRunQueryService queries;
  private final ObjectMapper objectMapper;

  public WorkflowController(
      WorkflowCoordinator coordinator, WorkflowRunQueryService queries, ObjectMapper objectMapper) {
    this.coordinator = coordinator;
    this.queries = queries;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/sofascore-season-sync")
  public WorkflowRunResponse start(@Valid @RequestBody StartSofaScoreSeasonRequest request)
      throws JsonProcessingException {
    SofaScoreSeasonWorkflowRequest payload =
        new SofaScoreSeasonWorkflowRequest(
            request.idempotencyKey(), request.tournamentId(), request.sofaSeasonId(), request.canonicalSeasonId());
    var run =
        coordinator.start(
            WorkflowConfiguration.SOFASCORE_SEASON_SYNC,
            payload.idempotencyKey(),
            objectMapper.writeValueAsString(payload));
    return WorkflowRunResponse.from(run, queries.findStages(run.getId()));
  }

  @PostMapping("/{id}/resume")
  public WorkflowRunResponse resume(@PathVariable Long id) {
    var run = coordinator.resume(id);
    return WorkflowRunResponse.from(run, queries.findStages(run.getId()));
  }

  @GetMapping("/{id}")
  public WorkflowRunResponse get(@PathVariable Long id) {
    var run = queries.getRun(id);
    return WorkflowRunResponse.from(run, queries.findStages(run.getId()));
  }

  public record StartSofaScoreSeasonRequest(
      @NotBlank String idempotencyKey,
      @Positive long tournamentId,
      @Positive long sofaSeasonId,
      @Positive long canonicalSeasonId) {}

  public record WorkflowRunResponse(
      Long id,
      String workflowType,
      String idempotencyKey,
      String status,
      String currentStage,
      String lastError,
      Instant createdAt,
      Instant startedAt,
      Instant completedAt,
      List<WorkflowStageResponse> stages) {
    static WorkflowRunResponse from(
        com.h2football.module.workflow.domain.model.WorkflowRun run,
        List<com.h2football.module.workflow.domain.model.WorkflowStageExecution> stages) {
      return new WorkflowRunResponse(
          run.getId(),
          run.getWorkflowType(),
          run.getIdempotencyKey(),
          run.getStatus().name(),
          run.getCurrentStage(),
          run.getLastError(),
          run.getCreatedAt(),
          run.getStartedAt(),
          run.getCompletedAt(),
          stages.stream()
              .map(
                  stage ->
                      new WorkflowStageResponse(
                          stage.getStageKey(),
                          stage.getStageOrder(),
                          stage.getStatus().name(),
                          stage.getAttemptCount(),
                          stage.getLastError(),
                          stage.getStartedAt(),
                          stage.getCompletedAt()))
              .toList());
    }
  }

  public record WorkflowStageResponse(
      String stageKey,
      int order,
      String status,
      int attempts,
      String lastError,
      Instant startedAt,
      Instant completedAt) {}
}
