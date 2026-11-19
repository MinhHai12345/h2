package com.h2football.module.workflow.bootstrap;

import com.h2football.module.workflow.application.SofaScoreSeasonIngestionStageHandler;
import com.h2football.module.workflow.application.WorkflowDefinition;
import com.h2football.module.workflow.application.WorkflowStageDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfiguration {
  public static final String SOFASCORE_SEASON_SYNC = "SOFASCORE_SEASON_SYNC";

  @Bean
  WorkflowDefinition sofaScoreSeasonWorkflowDefinition(SofaScoreSeasonIngestionStageHandler handler) {
    return new WorkflowDefinition(
        SOFASCORE_SEASON_SYNC,
        java.util.List.of(new WorkflowStageDefinition("SEASON_INGESTION", handler)));
  }
}
