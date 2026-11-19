package com.h2football.bootstrap.configuration;

import com.h2football.module.analytics.scheduling.AnalyticsSchedulerProperties;
import com.h2football.module.analytics.scheduling.MatchAnalyticsRefreshJob;
import com.h2football.module.analytics.scheduling.ModelEvaluationJob;
import java.util.TimeZone;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "app.analytics.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AnalyticsQuartzConfiguration {
  @Bean
  public SchedulerFactoryBeanCustomizer quartzJobFactoryCustomizer(
      AutowireCapableBeanFactory beanFactory) {
    return schedulerFactory ->
        schedulerFactory.setJobFactory(new AutowiringSpringBeanJobFactory(beanFactory));
  }

  @Bean
  public JobDetail matchAnalyticsRefreshJobDetail() {
    return JobBuilder.newJob(MatchAnalyticsRefreshJob.class)
        .withIdentity("matchAnalyticsRefreshJob")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger matchAnalyticsRefreshTrigger(
      @org.springframework.beans.factory.annotation.Qualifier("matchAnalyticsRefreshJobDetail")
          JobDetail jobDetail,
      AnalyticsSchedulerProperties properties) {
    return TriggerBuilder.newTrigger()
        .forJob(jobDetail)
        .withIdentity("matchAnalyticsRefreshTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule(properties.getMatchRefreshCron())
                .inTimeZone(TimeZone.getTimeZone("UTC"))
                .withMisfireHandlingInstructionDoNothing())
        .build();
  }

  @Bean
  public JobDetail modelEvaluationJobDetail() {
    return JobBuilder.newJob(ModelEvaluationJob.class)
        .withIdentity("modelEvaluationJob")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger modelEvaluationTrigger(
      @org.springframework.beans.factory.annotation.Qualifier("modelEvaluationJobDetail")
          JobDetail jobDetail,
      AnalyticsSchedulerProperties properties) {
    return TriggerBuilder.newTrigger()
        .forJob(jobDetail)
        .withIdentity("modelEvaluationTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule(properties.getModelEvaluationCron())
                .inTimeZone(TimeZone.getTimeZone("UTC"))
                .withMisfireHandlingInstructionDoNothing())
        .build();
  }
}
