package com.h2football.module.workflow.bootstrap;

import com.h2football.module.workflow.application.SofaScoreSeasonIngestionStageHandler;
import com.h2football.module.workflow.application.WorkflowDefinition;
import com.h2football.module.workflow.application.WorkflowStageDefinition;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class WorkflowConfiguration {
  public static final String SOFASCORE_SEASON_SYNC = "SOFASCORE_SEASON_SYNC";

  @Bean
  WorkflowDefinition sofaScoreSeasonWorkflowDefinition(
      SofaScoreSeasonIngestionStageHandler handler) {
    return new WorkflowDefinition(
        SOFASCORE_SEASON_SYNC,
        List.of(new WorkflowStageDefinition("SEASON_INGESTION", handler)));
  }
}
