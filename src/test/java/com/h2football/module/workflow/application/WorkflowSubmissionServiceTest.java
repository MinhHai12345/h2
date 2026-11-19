package com.h2football.module.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.workflow.bootstrap.WorkflowConfiguration;
import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowSubmissionServiceTest {
  @Mock private WorkflowDefinitionRegistry definitions;
  @Mock private WorkflowCheckpointService checkpoints;
  @Mock private WorkflowExecutionScheduler scheduler;
  @Mock private SeasonRepository seasons;

  @Test
  void shouldPersistAndScheduleOneIndependentWorkflowPerSeason() {
    Season season = mock(Season.class);
    WorkflowRun run = mock(WorkflowRun.class);
    when(seasons.findById(42L)).thenReturn(Optional.of(season));
    when(definitions.getRequired(WorkflowConfiguration.SOFASCORE_SEASON_SYNC))
        .thenReturn(mock(WorkflowDefinition.class));
    when(checkpoints.createOrLoad(
            eq(WorkflowConfiguration.SOFASCORE_SEASON_SYNC), eq("league-7-season-42"), anyString()))
        .thenReturn(run);
    when(run.getId()).thenReturn(123L);
    when(run.getStatus()).thenReturn(WorkflowRunStatus.PENDING);
    when(checkpoints.getRun(123L)).thenReturn(run);

    WorkflowSubmissionService service =
        new WorkflowSubmissionService(
            definitions, checkpoints, scheduler, seasons, new ObjectMapper());

    var submitted =
        service.submit(
            new SofaScoreSeasonWorkflowRequest("league-7-season-42", 17L, 52186L, 42L));

    assertThat(submitted).isSameAs(run);
    verify(scheduler).schedule(123L);
  }

  @Test
  void shouldNotScheduleAWorkflowThatHasAlreadySucceeded() {
    Season season = mock(Season.class);
    WorkflowRun run = mock(WorkflowRun.class);
    when(seasons.findById(42L)).thenReturn(Optional.of(season));
    when(definitions.getRequired(WorkflowConfiguration.SOFASCORE_SEASON_SYNC))
        .thenReturn(mock(WorkflowDefinition.class));
    when(checkpoints.createOrLoad(
            eq(WorkflowConfiguration.SOFASCORE_SEASON_SYNC), eq("league-7-season-42"), anyString()))
        .thenReturn(run);
    when(run.getId()).thenReturn(123L);
    when(run.getStatus()).thenReturn(WorkflowRunStatus.SUCCEEDED);
    when(checkpoints.getRun(123L)).thenReturn(run);

    WorkflowSubmissionService service =
        new WorkflowSubmissionService(
            definitions, checkpoints, scheduler, seasons, new ObjectMapper());

    service.submit(new SofaScoreSeasonWorkflowRequest("league-7-season-42", 17L, 52186L, 42L));

    verify(scheduler, never()).schedule(123L);
  }
}
