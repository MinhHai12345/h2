package com.h2football.module.workflow.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProviderSeasonWorkflowSubmissionService {
  public static final String WORKFLOW_TYPE = "PROVIDER_SEASON_SYNC";

  private final WorkflowDefinitionRegistry definitions;
  private final WorkflowCheckpointService checkpoints;
  private final WorkflowExecutionScheduler scheduler;
  private final SeasonRepository seasons;
  private final ObjectMapper objectMapper;

  public ProviderSeasonWorkflowSubmissionService(
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

  public WorkflowRun submit(ProviderSeasonWorkflowRequest request) {
    seasons
        .findById(request.canonicalSeasonId())
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unknown canonical season " + request.canonicalSeasonId()));
    definitions.getRequired(WORKFLOW_TYPE);
    String payload;
    try {
      payload = objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize provider workflow request", exception);
    }
    WorkflowRun run = checkpoints.createOrLoad(WORKFLOW_TYPE, request.idempotencyKey(), payload);
    if (run.getStatus() != WorkflowRunStatus.SUCCEEDED) scheduler.schedule(run.getId());
    return checkpoints.getRun(run.getId());
  }

  public List<WorkflowRun> submitBatch(List<ProviderSeasonWorkflowRequest> requests) {
    if (requests == null || requests.isEmpty() || requests.size() > 50) {
      throw new IllegalArgumentException("A provider batch must contain between 1 and 50 seasons");
    }
    return requests.stream().map(this::submit).toList();
  }
}
