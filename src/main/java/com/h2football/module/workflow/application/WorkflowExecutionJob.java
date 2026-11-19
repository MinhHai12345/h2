package com.h2football.module.workflow.application;

import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class WorkflowExecutionJob extends QuartzJobBean {
  private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionJob.class);

  private final WorkflowCoordinator coordinator;

  public WorkflowExecutionJob(WorkflowCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    long runId = context.getMergedJobDataMap().getLongValue(WorkflowExecutionScheduler.RUN_ID);
    log.info("Executing workflow run: runId={}", runId);
    try {
      var run = coordinator.resume(runId);
      if (run.getStatus() == WorkflowRunStatus.FAILED) {
        throw new JobExecutionException(
            "Workflow run failed: runId=" + runId + ", error=" + run.getLastError(), false);
      }
      log.info("Workflow execution finished: runId={}, status={}", runId, run.getStatus());
    } catch (JobExecutionException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      log.error("Workflow execution job failed: runId={}", runId, exception);
      throw new JobExecutionException("Workflow execution failed for run " + runId, exception, false);
    }
  }
}
