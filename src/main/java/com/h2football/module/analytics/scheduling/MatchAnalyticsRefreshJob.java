package com.h2football.module.analytics.scheduling;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class MatchAnalyticsRefreshJob implements Job {
  private static final Logger log = LoggerFactory.getLogger(MatchAnalyticsRefreshJob.class);

  @Autowired private AnalyticsSchedulingApplicationService scheduling;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      scheduling.refreshUpcomingMatches();
    } catch (RuntimeException exception) {
      log.error("Scheduled match analytics refresh failed", exception);
      throw new JobExecutionException(exception, false);
    }
  }
}
