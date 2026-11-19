package com.h2football.module.workflow.scheduling;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.workflow.application.ProviderSeasonWorkflowRequest;
import com.h2football.module.workflow.application.ProviderSeasonWorkflowSubmissionService;
import java.util.Date;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class ProviderSeasonWorkflowDispatchJob extends QuartzJobBean {
  private static final Logger log =
      LoggerFactory.getLogger(ProviderSeasonWorkflowDispatchJob.class);

  private final ProviderSeasonWorkflowSubmissionService submissions;

  public ProviderSeasonWorkflowDispatchJob(ProviderSeasonWorkflowSubmissionService submissions) {
    this.submissions = submissions;
  }

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    var data = context.getMergedJobDataMap();
    Provider provider = Provider.valueOf(data.getString("provider"));
    long seasonId = data.getLongValue("canonicalSeasonId");
    String externalLeagueId = data.getString("externalLeagueId");
    int seasonYear = data.getIntValue("seasonYear");
    Date scheduledAt =
        context.getScheduledFireTime() == null
            ? context.getFireTime()
            : context.getScheduledFireTime();
    String idempotencyKey =
        provider.name().toLowerCase()
            + "-season-"
            + seasonId
            + "-"
            + scheduledAt.getTime();
    try {
      var run =
          submissions.submit(
              new ProviderSeasonWorkflowRequest(
                  idempotencyKey, provider, seasonId, externalLeagueId, seasonYear));
      log.info(
          "Dispatched provider season workflow: runId={}, provider={}, seasonId={}",
          run.getId(),
          provider,
          seasonId);
    } catch (RuntimeException exception) {
      log.error(
          "Unable to dispatch provider season workflow: provider={}, seasonId={}",
          provider,
          seasonId,
          exception);
      throw new JobExecutionException("Unable to dispatch provider season workflow", exception);
    }
  }
}
