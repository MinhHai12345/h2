package com.h2football.module.operations.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.operations.domain.model.CronJob;
import com.h2football.module.operations.domain.model.CronJobStatus;
import com.h2football.module.operations.domain.repository.CronJobRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CronJobApplicationService {
  private static final Set<String> ALLOWED_JOB_CLASSES =
      Set.of(
          "com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob",
          "com.h2football.module.workflow.scheduling.SofaScoreSeasonWorkflowDispatchJob",
          "com.h2football.module.workflow.scheduling.ProviderSeasonWorkflowDispatchJob",
          "com.h2football.module.ingestion.scheduling.SofaScoreLeagueCatalogueSyncJob");
  private static final Set<String> TRIGGER_PARAMETERS =
      Set.of("tournamentId", "sofaSeasonId", "canonicalSeasonId");

  private final Scheduler scheduler;
  private final CronJobRepository repository;
  private final ObjectMapper objectMapper;

  public CronJobApplicationService(
      Scheduler scheduler, CronJobRepository repository, ObjectMapper objectMapper) {
    this.scheduler = scheduler;
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public SchedulerMetaData getSchedulerMetadata() {
    try {
      return scheduler.getMetaData();
    } catch (SchedulerException exception) {
      throw new CronJobOperationException("Unable to read Quartz scheduler metadata", exception);
    }
  }

  @Transactional(readOnly = true)
  public List<CronJob> findAll() {
    return repository.findAll();
  }

  @Transactional(readOnly = true)
  public CronJob get(Long id) {
    return repository.findById(id).orElseThrow(() -> new CronJobNotFoundException(id));
  }

  @Transactional
  public CronJob registerOrUpdate(
      String name,
      String group,
      String jobClass,
      String cronExpression,
      String description,
      JobDataMap jobData) {
    validateDefinition(name, group, jobClass, cronExpression);
    CronJob job =
        repository
            .findByJobNameAndJobGroup(name, group)
            .orElseGet(() -> new CronJob(name, group, jobClass, cronExpression, description));
    job.updateDefinition(jobClass, cronExpression, description);
    if (jobData != null) {
      job.updateJobData(serializeJobData(jobData));
    }
    CronJob saved = repository.save(job);
    schedule(saved, jobData == null ? deserializeJobData(saved.getJobDataJson()) : jobData);
    return saved;
  }

  @Transactional
  public CronJob updateSchedule(Long id, String cronExpression) {
    CronJob job = get(id);
    validateCron(cronExpression);
    JobKey key = key(job);
    try {
      if (!scheduler.checkExists(key)) {
        schedule(job, deserializeJobData(job.getJobDataJson()));
      }
      Trigger trigger = buildTrigger(job, cronExpression);
      if (scheduler.checkExists(triggerKey(job))) {
        scheduler.rescheduleJob(triggerKey(job), trigger);
      } else {
        scheduler.scheduleJob(trigger);
      }
      job.updateDefinition(job.getJobClass(), cronExpression, job.getDescription());
      if (job.getStatus() != CronJobStatus.PAUSED) {
        job.markScheduled();
      } else {
        scheduler.pauseJob(key);
      }
      return repository.save(job);
    } catch (SchedulerException exception) {
      throw new CronJobOperationException("Unable to update cron job schedule", exception);
    }
  }

  @Transactional
  public CronJob pause(Long id) {
    CronJob job = get(id);
    try {
      scheduler.pauseJob(key(job));
      job.markPaused();
      return repository.save(job);
    } catch (SchedulerException exception) {
      throw new CronJobOperationException("Unable to pause cron job " + id, exception);
    }
  }

  @Transactional
  public CronJob resume(Long id) {
    CronJob job = get(id);
    try {
      scheduler.resumeJob(key(job));
      job.markScheduled();
      return repository.save(job);
    } catch (SchedulerException exception) {
      throw new CronJobOperationException("Unable to resume cron job " + id, exception);
    }
  }

  public CronJob triggerNow(Long id, Map<String, Long> parameters) {
    CronJob job = get(id);
    if (job.getStatus() == CronJobStatus.PAUSED) {
      throw new CronJobOperationException("A paused cron job must be resumed before triggering");
    }
    JobDataMap triggerData = new JobDataMap();
    if (parameters != null) {
      parameters.forEach(
          (name, value) -> {
            if (!TRIGGER_PARAMETERS.contains(name) || value == null || value <= 0) {
              throw new IllegalArgumentException("Invalid one-time job parameter: " + name);
            }
            triggerData.put(name, value);
          });
    }
    try {
      ensureScheduled(job);
      job.markRunning();
      repository.save(job);
      try {
        scheduler.triggerJob(key(job), triggerData);
      } catch (SchedulerException triggerFailure) {
        job.markScheduled();
        repository.save(job);
        throw triggerFailure;
      }
      return job;
    } catch (SchedulerException exception) {
      throw new CronJobOperationException("Unable to trigger cron job " + id, exception);
    }
  }

  @Transactional
  public CronJob stop(Long id) {
    CronJob job = get(id);
    try {
      boolean interrupted = scheduler.interrupt(key(job));
      if (!interrupted) {
        throw new CronJobOperationException("Cron job " + id + " has no interruptible execution");
      }
      return job;
    } catch (SchedulerException exception) {
      throw new CronJobOperationException("Unable to stop cron job " + id, exception);
    }
  }

  @Transactional
  public void pauseGroup(String group) {
    repository.findAll().stream()
        .filter(job -> job.getJobGroup().equals(group))
        .forEach(
            job -> {
              try {
                scheduler.pauseJob(key(job));
                job.markPaused();
                repository.save(job);
              } catch (SchedulerException exception) {
                throw new CronJobOperationException(
                    "Unable to pause cron job " + job.getJobName(), exception);
              }
            });
  }

  @Transactional
  public void deleteGroupExcept(String group, Set<String> activeJobNames) {
    repository.findAll().stream()
        .filter(job -> job.getJobGroup().equals(group))
        .filter(job -> !activeJobNames.contains(job.getJobName()))
        .map(CronJob::getId)
        .toList()
        .forEach(this::delete);
  }

  @Transactional
  public boolean delete(Long id) {
    CronJob job = get(id);
    try {
      boolean deleted = scheduler.deleteJob(key(job));
      repository.delete(job);
      return deleted;
    } catch (SchedulerException exception) {
      throw new CronJobOperationException("Unable to delete cron job " + id, exception);
    }
  }

  private void schedule(CronJob job, JobDataMap jobData) {
    try {
      JobKey key = key(job);
      JobDetail detail =
          JobBuilder.newJob(resolveJobClass(job.getJobClass()))
              .withIdentity(key)
              .usingJobData(jobData == null ? new JobDataMap() : jobData)
              .storeDurably()
              .build();
      Trigger trigger = buildTrigger(job, job.getCronExpression());
      if (scheduler.checkExists(key)) {
        scheduler.addJob(detail, true, true);
        if (scheduler.checkExists(triggerKey(job))) {
          scheduler.rescheduleJob(triggerKey(job), trigger);
        } else {
          scheduler.scheduleJob(trigger);
        }
      } else {
        scheduler.scheduleJob(detail, trigger);
      }
      if (job.getStatus() == CronJobStatus.PAUSED) {
        scheduler.pauseJob(key);
      } else {
        job.markScheduled();
        repository.save(job);
      }
    } catch (SchedulerException exception) {
      throw new CronJobOperationException(
          "Unable to register cron job " + job.getJobName(), exception);
    }
  }

  private void ensureScheduled(CronJob job) throws SchedulerException {
    JobKey key = key(job);
    if (!scheduler.checkExists(key)) {
      schedule(job, deserializeJobData(job.getJobDataJson()));
    }
  }

  private Trigger buildTrigger(CronJob job, String expression) {
    return TriggerBuilder.newTrigger()
        .withIdentity(triggerKey(job))
        .forJob(key(job))
        .withSchedule(
            CronScheduleBuilder.cronSchedule(expression)
                .inTimeZone(java.util.TimeZone.getTimeZone("UTC"))
                .withMisfireHandlingInstructionDoNothing())
        .build();
  }

  private String serializeJobData(JobDataMap jobData) {
    try {
      return objectMapper.writeValueAsString(jobData.getWrappedMap());
    } catch (JsonProcessingException exception) {
      throw new CronJobOperationException("Unable to serialize cron job parameters", exception);
    }
  }

  private JobDataMap deserializeJobData(String json) {
    if (json == null || json.isBlank()) {
      return new JobDataMap();
    }
    try {
      Map<String, Object> values =
          objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
      values.replaceAll(
          (key, value) -> value instanceof Number number ? number.longValue() : value);
      return new JobDataMap(values);
    } catch (JsonProcessingException exception) {
      throw new CronJobOperationException("Unable to restore cron job parameters", exception);
    }
  }

  private Class<? extends org.quartz.Job> resolveJobClass(String className) {
    if (!ALLOWED_JOB_CLASSES.contains(className)) {
      throw new IllegalArgumentException("Unsupported cron job class: " + className);
    }
    try {
      return Class.forName(className).asSubclass(org.quartz.Job.class);
    } catch (ClassNotFoundException exception) {
      throw new IllegalArgumentException("Cron job class does not exist: " + className, exception);
    }
  }

  private void validateDefinition(String name, String group, String jobClass, String cron) {
    if (name == null || name.isBlank() || group == null || group.isBlank()) {
      throw new IllegalArgumentException("Cron job name and group are required");
    }
    resolveJobClass(jobClass);
    validateCron(cron);
  }

  private void validateCron(String expression) {
    if (expression == null || !CronExpression.isValidExpression(expression)) {
      throw new IllegalArgumentException("Invalid Quartz cron expression: " + expression);
    }
  }

  private JobKey key(CronJob job) {
    return JobKey.jobKey(job.getJobName(), job.getJobGroup());
  }

  private TriggerKey triggerKey(CronJob job) {
    return TriggerKey.triggerKey(job.getJobName(), job.getJobGroup());
  }
}
