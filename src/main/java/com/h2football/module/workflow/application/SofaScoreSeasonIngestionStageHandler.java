package com.h2football.module.workflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.ingestion.application.SofaScoreSeasonMatchIngestionService;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreSeasonIngestionStageHandler implements WorkflowStageHandler {
  private final ObjectMapper objectMapper;
  private final SofaScoreSeasonMatchIngestionService ingestion;

  public SofaScoreSeasonIngestionStageHandler(
      ObjectMapper objectMapper, SofaScoreSeasonMatchIngestionService ingestion) {
    this.objectMapper = objectMapper;
    this.ingestion = ingestion;
  }

  @Override
  public String execute(WorkflowExecutionContext context) throws Exception {
    SofaScoreSeasonWorkflowRequest request =
        objectMapper.readValue(context.requestPayload(), SofaScoreSeasonWorkflowRequest.class);
    var result =
        ingestion.sync(request.tournamentId(), request.sofaSeasonId(), request.canonicalSeasonId());
    return objectMapper.writeValueAsString(
        new IngestionCheckpoint(
            result.discoveredEvents(),
            result.createdMatches(),
            result.updatedMatches(),
            result.skippedEvents(),
            result.uniqueEvents(),
            result.matchDetails().statisticsSucceeded(),
            result.matchDetails().lineupsSucceeded(),
            result.matchDetails().incidentsSucceeded(),
            result.matchDetails().playerStatisticsSucceeded()));
  }

  public record IngestionCheckpoint(
      int discoveredEvents,
      int createdMatches,
      int updatedMatches,
      int skippedEvents,
      int uniqueEvents,
      int statisticsSucceeded,
      int lineupsSucceeded,
      int incidentsSucceeded,
      int playerStatisticsSucceeded) {}
}
