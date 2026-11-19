package com.h2football.infrastructure.scheduling;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/** Creates Quartz jobs through Spring to support constructor injection. */
public class CronJobFactory extends SpringBeanJobFactory {
  private final AutowireCapableBeanFactory beanFactory;

  public CronJobFactory(AutowireCapableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  @NonNull
  protected Object createJobInstance(@NonNull TriggerFiredBundle bundle) {
    return beanFactory.createBean(bundle.getJobDetail().getJobClass());
  }
}
