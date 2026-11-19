package com.h2football.module.operations.domain.repository;

import com.h2football.module.operations.domain.model.CronJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CronJobRepository extends JpaRepository<CronJob, Long> {
  Optional<CronJob> findByJobNameAndJobGroup(String jobName, String jobGroup);
}
