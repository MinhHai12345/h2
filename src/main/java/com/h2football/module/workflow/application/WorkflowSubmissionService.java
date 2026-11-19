package com.h2football.module.workflow.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.workflow.bootstrap.WorkflowConfiguration;
import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkflowSubmissionService {
  private final WorkflowDefinitionRegistry definitions;
  private final WorkflowCheckpointService checkpoints;
  private final WorkflowExecutionScheduler scheduler;
  private final SeasonRepository seasons;
  private final ObjectMapper objectMapper;

  public WorkflowSubmissionService(
      WorkflowDefinitionRegistry definitions,
      WorkflowCheckpointService checkpoints,
      WorkflowExecutionScheduler scheduler,
      SeasonRepository seasons,
      ObjectMapper objectMapper) {
    this.definitions = definitions;
    this.checkpoints = checkpoints;
    this.scheduler = scheduler;
    this.seasons = seasons;
    this.objectMapper = objectMapper;
  }

  public WorkflowRun submit(SofaScoreSeasonWorkflowRequest request) {
    return submit(request, null);
  }

  public WorkflowRun submit(SofaScoreSeasonWorkflowRequest request, Long expectedLeagueId) {
    validateSeason(request.canonicalSeasonId(), expectedLeagueId);
    definitions.getRequired(WorkflowConfiguration.SOFASCORE_SEASON_SYNC);
    String payload = serialize(request);
    WorkflowRun run =
        checkpoints.createOrLoad(
            WorkflowConfiguration.SOFASCORE_SEASON_SYNC, request.idempotencyKey(), payload);
    if (run.getStatus() != WorkflowRunStatus.SUCCEEDED) {
      scheduler.schedule(run.getId());
    }
    return checkpoints.getRun(run.getId());
  }

  public List<WorkflowRun> submitBatch(List<SeasonPipelineRequest> requests) {
    if (requests == null || requests.isEmpty() || requests.size() > 50) {
      throw new IllegalArgumentException("A batch must contain between 1 and 50 seasons");
    }
    requests.forEach(request -> validateSeason(request.seasonId(), request.leagueId()));
    return requests.stream()
        .map(
            request ->
                submit(
                    new SofaScoreSeasonWorkflowRequest(
                        request.idempotencyKey(),
                        request.tournamentId(),
                        request.sofaSeasonId(),
                        request.seasonId()),
                    request.leagueId()))
        .toList();
  }

  public WorkflowRun resume(Long workflowRunId) {
    WorkflowRun run = checkpoints.getRun(workflowRunId);
    if (run.getStatus() != WorkflowRunStatus.SUCCEEDED) {
      scheduler.schedule(workflowRunId);
    }
    return checkpoints.getRun(workflowRunId);
  }

  private void validateSeason(Long seasonId, Long expectedLeagueId) {
    var season =
        seasons
            .findById(seasonId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown season " + seasonId));
    if (expectedLeagueId != null && !season.getLeague().getId().equals(expectedLeagueId)) {
      throw new IllegalArgumentException(
          "Season " + seasonId + " does not belong to league " + expectedLeagueId);
    }
  }

  private String serialize(SofaScoreSeasonWorkflowRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize workflow request", exception);
    }
  }

  public record SeasonPipelineRequest(
      String idempotencyKey,
      long leagueId,
      long seasonId,
      long tournamentId,
      long sofaSeasonId) {
    public SeasonPipelineRequest {
      if (idempotencyKey == null
          || idempotencyKey.isBlank()
          || leagueId <= 0
          || seasonId <= 0
          || tournamentId <= 0
          || sofaSeasonId <= 0) {
        throw new IllegalArgumentException("Season pipeline identifiers are required and positive");
      }
    }
  }
}
