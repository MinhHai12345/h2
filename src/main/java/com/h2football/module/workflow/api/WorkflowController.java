package com.h2football.module.workflow.api;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.workflow.application.ProviderSeasonWorkflowRequest;
import com.h2football.module.workflow.application.ProviderSeasonWorkflowSubmissionService;
import com.h2football.module.workflow.application.SofaScoreSeasonWorkflowRequest;
import com.h2football.module.workflow.application.WorkflowRunQueryService;
import com.h2football.module.workflow.application.WorkflowSubmissionService;
import com.h2football.module.workflow.application.WorkflowSubmissionService.SeasonPipelineRequest;
import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowStageExecution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
  private final WorkflowSubmissionService submissions;
  private final ProviderSeasonWorkflowSubmissionService providerSubmissions;
  private final WorkflowRunQueryService queries;

  public WorkflowController(
      WorkflowSubmissionService submissions,
      ProviderSeasonWorkflowSubmissionService providerSubmissions,
      WorkflowRunQueryService queries) {
    this.submissions = submissions;
    this.providerSubmissions = providerSubmissions;
    this.queries = queries;
  }

  @PostMapping("/sofascore-season-sync")
  public WorkflowRunResponse start(@Valid @RequestBody StartSofaScoreSeasonRequest request) {
    SofaScoreSeasonWorkflowRequest payload =
        new SofaScoreSeasonWorkflowRequest(
            request.idempotencyKey(),
            request.tournamentId(),
            request.sofaSeasonId(),
            request.canonicalSeasonId());
    var run = submissions.submit(payload);
    return WorkflowRunResponse.from(run, queries.findStages(run.getId()));
  }

  @PostMapping("/sofascore-season-pipelines")
  public List<WorkflowRunResponse> startBatch(
      @Valid @RequestBody StartSeasonPipelineBatchRequest request) {
    return submissions.submitBatch(request.seasons()).stream()
        .map(run -> WorkflowRunResponse.from(run, queries.findStages(run.getId())))
        .toList();
  }

  @PostMapping("/provider-season-sync")
  public WorkflowRunResponse startProviderSeason(
      @Valid @RequestBody StartProviderSeasonRequest request) {
    var payload =
        new ProviderSeasonWorkflowRequest(
            request.idempotencyKey(),
            request.provider(),
            request.canonicalSeasonId(),
            request.externalLeagueId(),
            request.seasonYear());
    var run = providerSubmissions.submit(payload);
    return WorkflowRunResponse.from(run, queries.findStages(run.getId()));
  }

  @PostMapping("/provider-season-pipelines")
  public List<WorkflowRunResponse> startProviderBatch(
      @Valid @RequestBody StartProviderSeasonBatchRequest request) {
    return providerSubmissions.submitBatch(request.seasons()).stream()
        .map(run -> WorkflowRunResponse.from(run, queries.findStages(run.getId())))
        .toList();
  }

  @PostMapping("/{id}/resume")
  public WorkflowRunResponse resume(@PathVariable Long id) {
    var run = submissions.resume(id);
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

  public record StartSeasonPipelineBatchRequest(
      @Size(min = 1, max = 50) List<@Valid SeasonPipelineRequest> seasons) {}

  public record StartProviderSeasonRequest(
      @NotBlank String idempotencyKey,
      @NotNull Provider provider,
      @Positive long canonicalSeasonId,
      @NotBlank String externalLeagueId,
      @Positive int seasonYear) {}

  public record StartProviderSeasonBatchRequest(
      @Size(min = 1, max = 50) List<@Valid ProviderSeasonWorkflowRequest> seasons) {}

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
    static WorkflowRunResponse from(WorkflowRun run, List<WorkflowStageExecution> stages) {
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
