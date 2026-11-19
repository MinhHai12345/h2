package com.h2football.module.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionSchedulerTest {
  @Mock private Scheduler scheduler;

  @Test
  void shouldCreateRecoverableQuartzJobForNewWorkflowRun() throws Exception {
    when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
    WorkflowExecutionScheduler executionScheduler = new WorkflowExecutionScheduler(scheduler);

    executionScheduler.schedule(123L);

    ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
    assertThat(jobCaptor.getValue().getJobClass()).isEqualTo(WorkflowExecutionJob.class);
    assertThat(jobCaptor.getValue().requestsRecovery()).isTrue();
    assertThat(jobCaptor.getValue().getJobDataMap().getLong(WorkflowExecutionScheduler.RUN_ID))
        .isEqualTo(123L);
    assertThat(triggerCaptor.getValue().getJobKey()).isEqualTo(jobCaptor.getValue().getKey());
  }

  @Test
  void shouldNotQueueDuplicateTriggerWhileJobIsAlreadyQueued() throws Exception {
    when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
    when(scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of());
    when(scheduler.getTriggersOfJob(any(JobKey.class))).thenReturn(List.of());
    WorkflowExecutionScheduler executionScheduler = new WorkflowExecutionScheduler(scheduler);

    executionScheduler.schedule(123L);

    verify(scheduler).triggerJob(any(JobKey.class), any());
    verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
  }
}
