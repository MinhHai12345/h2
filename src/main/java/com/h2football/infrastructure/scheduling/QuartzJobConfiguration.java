package com.h2football.infrastructure.scheduling;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/** Enables constructor injection in Quartz jobs while keeping jobs outside web controllers. */
@Configuration
public class QuartzJobConfiguration {
  @Bean
  SchedulerFactoryBeanCustomizer quartzJobFactoryCustomizer(
      AutowireCapableBeanFactory beanFactory) {
    return schedulerFactoryBean ->
        schedulerFactoryBean.setJobFactory(
            new SpringBeanJobFactory() {
              @Override
              protected Object createJobInstance(
                  org.springframework.scheduling.quartz.TriggerFiredBundle bundle) {
                return beanFactory.createBean(bundle.getJobDetail().getJobClass());
              }
            });
  }
}
