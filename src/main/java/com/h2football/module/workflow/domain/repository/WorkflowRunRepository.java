package com.h2football.module.workflow.domain.repository;

import com.h2football.module.workflow.domain.model.WorkflowRun;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, Long> {
  Optional<WorkflowRun> findByWorkflowTypeAndIdempotencyKey(String workflowType, String idempotencyKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select run from WorkflowRun run where run.id = :id")
  Optional<WorkflowRun> findByIdForUpdate(@Param("id") Long id);
}
