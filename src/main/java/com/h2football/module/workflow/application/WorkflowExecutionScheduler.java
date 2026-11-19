package com.h2football.module.workflow.application;

import java.util.List;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

@Service
public class WorkflowExecutionScheduler {
  public static final String JOB_GROUP = "workflow-execution";
  public static final String RUN_ID = "workflowRunId";

  private final Scheduler scheduler;

  public WorkflowExecutionScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public void schedule(Long workflowRunId) {
    if (workflowRunId == null || workflowRunId <= 0) {
      throw new IllegalArgumentException("workflowRunId must be positive");
    }
    JobKey key = jobKey(workflowRunId);
    try {
      if (scheduler.checkExists(key)) {
        boolean executing =
            scheduler.getCurrentlyExecutingJobs().stream()
                .anyMatch(item -> item.getJobDetail().getKey().equals(key));
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(key);
        if (!executing && triggers.isEmpty()) {
          scheduler.triggerJob(key, new JobDataMap());
        }
        return;
      }
      JobDetail detail =
          JobBuilder.newJob(WorkflowExecutionJob.class)
              .withIdentity(key)
              .usingJobData(RUN_ID, workflowRunId)
              .requestRecovery(true)
              .storeDurably()
              .build();
      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity("workflow-run-" + workflowRunId + "-trigger", JOB_GROUP)
              .forJob(detail)
              .startNow()
              .build();
      scheduler.scheduleJob(detail, trigger);
    } catch (SchedulerException exception) {
      throw new WorkflowSchedulingException(
          "Unable to schedule workflow run " + workflowRunId, exception);
    }
  }

  private JobKey jobKey(Long workflowRunId) {
    return JobKey.jobKey("workflow-run-" + workflowRunId, JOB_GROUP);
  }
}
