package com.h2football.module.workflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.workflow.application.DataReadinessService.DataReadinessReport;
import org.springframework.stereotype.Component;

@Component
public class ProviderDataReadinessStageHandler implements WorkflowStageHandler {
  private final ObjectMapper objectMapper;
  private final DataReadinessService readiness;

  public ProviderDataReadinessStageHandler(
      ObjectMapper objectMapper, DataReadinessService readiness) {
    this.objectMapper = objectMapper;
    this.readiness = readiness;
  }

  @Override
  public String execute(WorkflowExecutionContext context) throws Exception {
    ProviderSeasonWorkflowRequest request =
        objectMapper.readValue(context.requestPayload(), ProviderSeasonWorkflowRequest.class);
    DataReadinessReport report = readiness.inspect(request.canonicalSeasonId(), request.provider());
    if (!report.consistent()) {
      throw new WorkflowStageRejectedException(
          "Data readiness gate rejected season "
              + report.seasonId()
              + " from "
              + request.provider()
              + ": finishedMatchesWithoutScore="
              + report.finishedMatchesWithoutScore());
    }
    return objectMapper.writeValueAsString(report);
  }
}
