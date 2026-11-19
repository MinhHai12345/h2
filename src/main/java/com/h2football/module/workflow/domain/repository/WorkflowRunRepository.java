package com.h2football.module.workflow.domain.repository;

import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowRunStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, Long> {
  Optional<WorkflowRun> findByWorkflowTypeAndIdempotencyKey(
      String workflowType, String idempotencyKey);

  List<WorkflowRun> findTop20ByStatusOrderByCreatedAtAsc(WorkflowRunStatus status);

  List<WorkflowRun> findTop20ByStatusAndLeaseExpiresAtBeforeOrderByCreatedAtAsc(
      WorkflowRunStatus status, Instant leaseExpiresAt);

  @Modifying
  @Query(
      value =
          "INSERT INTO workflow_runs (workflow_type, idempotency_key, request_payload, status, created_at, version) "
              + "VALUES (:type, :key, :payload, 'PENDING', CURRENT_TIMESTAMP(6), 0) "
              + "ON DUPLICATE KEY UPDATE id = id",
      nativeQuery = true)
  int insertIfAbsent(
      @Param("type") String workflowType,
      @Param("key") String idempotencyKey,
      @Param("payload") String requestPayload);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select run from WorkflowRun run where run.id = :id")
  Optional<WorkflowRun> findByIdForUpdate(@Param("id") Long id);
}
