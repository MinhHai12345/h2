package com.h2football.module.workflow.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.identity.domain.model.Provider;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataReadinessStageHandlerTest {
  @Mock private DataReadinessService readiness;

  @Test
  void shouldRejectPipelineWhenSeasonHasNoConsistentMatchData() throws Exception {
    var report =
        new DataReadinessService.DataReadinessReport(
            42L,
            Provider.SOFASCORE,
            0,
            0,
            0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Instant.parse("2026-11-19T09:00:00Z"),
            60,
            false,
            false,
            "NEEDS_ATTENTION");
    when(readiness.inspect(42L, Provider.SOFASCORE)).thenReturn(report);
    var handler = new DataReadinessStageHandler(new ObjectMapper(), readiness);
    var context =
        new WorkflowExecutionContext(
            7L,
            "SOFASCORE_SEASON_SYNC",
            "DATA_READINESS",
            new ObjectMapper()
                .writeValueAsString(new SofaScoreSeasonWorkflowRequest("key", 17L, 52186L, 42L)),
            1);

    assertThatThrownBy(() -> handler.execute(context))
        .isInstanceOf(WorkflowStageRejectedException.class)
        .hasMessageContaining("totalMatches=0");
  }
}
