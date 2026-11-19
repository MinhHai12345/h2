package com.h2football.module.workflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.analytics.application.ModelEvaluationApplicationService;
import org.springframework.stereotype.Component;

@Component
public class ProviderModelEvaluationStageHandler implements WorkflowStageHandler {
  private final ObjectMapper objectMapper;
  private final ModelEvaluationApplicationService evaluation;

  public ProviderModelEvaluationStageHandler(
      ObjectMapper objectMapper, ModelEvaluationApplicationService evaluation) {
    this.objectMapper = objectMapper;
    this.evaluation = evaluation;
  }

  @Override
  public String execute(WorkflowExecutionContext context) throws Exception {
    ProviderSeasonWorkflowRequest request =
        objectMapper.readValue(context.requestPayload(), ProviderSeasonWorkflowRequest.class);
    var reports = evaluation.evaluateSeason(request.canonicalSeasonId(), request.provider());
    int predictions =
        reports.stream()
            .mapToInt(ModelEvaluationApplicationService.ModelEvaluationReport::predictionSnapshots)
            .sum();
    int evaluated =
        reports.stream()
            .mapToInt(
                ModelEvaluationApplicationService.ModelEvaluationReport::newlyEvaluatedSnapshots)
            .sum();
    return objectMapper.writeValueAsString(
        new ModelEvaluationStageHandler.ModelEvaluationCheckpoint(
            request.canonicalSeasonId(),
            request.provider(),
            reports.size(),
            predictions,
            evaluated));
  }
}
