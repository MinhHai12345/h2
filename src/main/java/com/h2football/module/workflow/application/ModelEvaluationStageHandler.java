package com.h2football.module.workflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.analytics.application.ModelEvaluationApplicationService;
import com.h2football.module.identity.domain.model.Provider;
import org.springframework.stereotype.Component;

@Component
public class ModelEvaluationStageHandler implements WorkflowStageHandler {
  private final ObjectMapper objectMapper;
  private final ModelEvaluationApplicationService evaluation;

  public ModelEvaluationStageHandler(
      ObjectMapper objectMapper, ModelEvaluationApplicationService evaluation) {
    this.objectMapper = objectMapper;
    this.evaluation = evaluation;
  }

  @Override
  public String execute(WorkflowExecutionContext context) throws Exception {
    SofaScoreSeasonWorkflowRequest request =
        objectMapper.readValue(context.requestPayload(), SofaScoreSeasonWorkflowRequest.class);
    var reports = evaluation.evaluateSeason(request.canonicalSeasonId(), Provider.SOFASCORE);
    int predictions = reports.stream().mapToInt(
        ModelEvaluationApplicationService.ModelEvaluationReport::predictionSnapshots).sum();
    int evaluated = reports.stream().mapToInt(
        ModelEvaluationApplicationService.ModelEvaluationReport::newlyEvaluatedSnapshots).sum();
    return objectMapper.writeValueAsString(
        new ModelEvaluationCheckpoint(
            request.canonicalSeasonId(), Provider.SOFASCORE, reports.size(), predictions, evaluated));
  }

  public record ModelEvaluationCheckpoint(
      Long seasonId, Provider provider, int modelVersions, int predictionSnapshots, int newlyEvaluated) {}
}
