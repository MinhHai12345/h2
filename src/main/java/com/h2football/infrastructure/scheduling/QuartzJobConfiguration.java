package com.h2football.infrastructure.scheduling;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzJobConfiguration {
  @Bean
  SchedulerFactoryBeanCustomizer quartzJobFactoryCustomizer(
      AutowireCapableBeanFactory beanFactory, CronJobStateListener stateListener) {
    return schedulerFactoryBean -> {
      schedulerFactoryBean.setJobFactory(new CronJobFactory(beanFactory));
      schedulerFactoryBean.setGlobalJobListeners(stateListener);
    };
  }
}
