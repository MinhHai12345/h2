package com.h2football.module.workflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.ingestion.application.MultiProviderSeasonIngestionService;
import org.springframework.stereotype.Component;

@Component
public class MultiProviderSeasonIngestionStageHandler implements WorkflowStageHandler {
  private final ObjectMapper objectMapper;
  private final MultiProviderSeasonIngestionService ingestion;

  public MultiProviderSeasonIngestionStageHandler(
      ObjectMapper objectMapper, MultiProviderSeasonIngestionService ingestion) {
    this.objectMapper = objectMapper;
    this.ingestion = ingestion;
  }

  @Override
  public String execute(WorkflowExecutionContext context) throws Exception {
    ProviderSeasonWorkflowRequest request =
        objectMapper.readValue(context.requestPayload(), ProviderSeasonWorkflowRequest.class);
    var result =
        ingestion.sync(
            request.provider(),
            request.canonicalSeasonId(),
            request.externalLeagueId(),
            request.seasonYear());
    return objectMapper.writeValueAsString(result);
  }
}
