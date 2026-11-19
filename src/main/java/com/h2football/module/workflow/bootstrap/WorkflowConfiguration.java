package com.h2football.module.workflow.bootstrap;

import com.h2football.module.workflow.application.AnalyticsRefreshStageHandler;
import com.h2football.module.workflow.application.DataReadinessStageHandler;
import com.h2football.module.workflow.application.ModelEvaluationStageHandler;
import com.h2football.module.workflow.application.MultiProviderSeasonIngestionStageHandler;
import com.h2football.module.workflow.application.ProviderAnalyticsRefreshStageHandler;
import com.h2football.module.workflow.application.ProviderDataReadinessStageHandler;
import com.h2football.module.workflow.application.ProviderModelEvaluationStageHandler;
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
  public static final String PROVIDER_SEASON_SYNC = "PROVIDER_SEASON_SYNC";

  @Bean
  WorkflowDefinition sofaScoreSeasonWorkflowDefinition(
      SofaScoreSeasonIngestionStageHandler ingestion,
      DataReadinessStageHandler readiness,
      AnalyticsRefreshStageHandler analytics,
      ModelEvaluationStageHandler evaluation) {
    return new WorkflowDefinition(
        SOFASCORE_SEASON_SYNC,
        List.of(
            new WorkflowStageDefinition("SEASON_INGESTION", ingestion),
            new WorkflowStageDefinition("DATA_READINESS", readiness),
            new WorkflowStageDefinition("ANALYTICS_REFRESH", analytics),
            new WorkflowStageDefinition("MODEL_EVALUATION", evaluation)));
  }

  @Bean
  WorkflowDefinition providerSeasonWorkflowDefinition(
      MultiProviderSeasonIngestionStageHandler ingestion,
      ProviderDataReadinessStageHandler readiness,
      ProviderAnalyticsRefreshStageHandler analytics,
      ProviderModelEvaluationStageHandler evaluation) {
    return new WorkflowDefinition(
        PROVIDER_SEASON_SYNC,
        List.of(
            new WorkflowStageDefinition("SEASON_INGESTION", ingestion),
            new WorkflowStageDefinition("DATA_READINESS", readiness),
            new WorkflowStageDefinition("ANALYTICS_REFRESH", analytics),
            new WorkflowStageDefinition("MODEL_EVALUATION", evaluation)));
  }
}
