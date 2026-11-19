package com.h2football.module.operations.application;

public class CronJobNotFoundException extends RuntimeException {
  public CronJobNotFoundException(Long id) {
    super("Cron job not found: " + id);
  }
}
