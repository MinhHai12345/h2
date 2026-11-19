package com.h2football.module.workflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.identity.domain.model.Provider;
import org.springframework.stereotype.Component;

@Component
public class DataReadinessStageHandler implements WorkflowStageHandler {
  private final ObjectMapper objectMapper;
  private final DataReadinessService readiness;

  public DataReadinessStageHandler(
      ObjectMapper objectMapper, DataReadinessService readiness) {
    this.objectMapper = objectMapper;
    this.readiness = readiness;
  }

  @Override
  public String execute(WorkflowExecutionContext context) throws Exception {
    SofaScoreSeasonWorkflowRequest request =
        objectMapper.readValue(context.requestPayload(), SofaScoreSeasonWorkflowRequest.class);
    var report = readiness.inspect(request.canonicalSeasonId(), Provider.SOFASCORE);
    return objectMapper.writeValueAsString(report);
  }
}
