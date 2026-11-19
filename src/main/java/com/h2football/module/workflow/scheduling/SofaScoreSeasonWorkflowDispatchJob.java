package com.h2football.module.workflow.scheduling;

import com.h2football.module.ingestion.scheduling.SofaScoreSeasonMatchSyncJob;
import com.h2football.module.workflow.application.SofaScoreSeasonWorkflowRequest;
import com.h2football.module.workflow.application.WorkflowSubmissionService;
import java.util.Date;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class SofaScoreSeasonWorkflowDispatchJob extends QuartzJobBean {
  private static final Logger log =
      LoggerFactory.getLogger(SofaScoreSeasonWorkflowDispatchJob.class);

  private final WorkflowSubmissionService submissions;

  public SofaScoreSeasonWorkflowDispatchJob(WorkflowSubmissionService submissions) {
    this.submissions = submissions;
  }

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    var parameters = context.getMergedJobDataMap();
    long tournamentId = parameters.getLongValue(SofaScoreSeasonMatchSyncJob.TOURNAMENT_ID);
    long sofaSeasonId = parameters.getLongValue(SofaScoreSeasonMatchSyncJob.SOFA_SEASON_ID);
    long canonicalSeasonId =
        parameters.getLongValue(SofaScoreSeasonMatchSyncJob.CANONICAL_SEASON_ID);
    Date scheduledAt =
        context.getScheduledFireTime() == null
            ? context.getFireTime()
            : context.getScheduledFireTime();
    String idempotencyKey = "sofascore-season-" + canonicalSeasonId + "-" + scheduledAt.getTime();

    try {
      var run =
          submissions.submit(
              new SofaScoreSeasonWorkflowRequest(
                  idempotencyKey, tournamentId, sofaSeasonId, canonicalSeasonId));
      log.info(
          "Dispatched SofaScore season workflow: runId={}, seasonId={}, tournamentId={}",
          run.getId(),
          canonicalSeasonId,
          tournamentId);
    } catch (RuntimeException exception) {
      log.error(
          "Unable to dispatch SofaScore season workflow: seasonId={}, tournamentId={}",
          canonicalSeasonId,
          tournamentId,
          exception);
      throw new JobExecutionException("Unable to dispatch SofaScore season workflow", exception);
    }
  }
}
