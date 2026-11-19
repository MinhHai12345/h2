package com.h2football.module.workflow.domain.repository;

import com.h2football.module.workflow.domain.model.WorkflowStageExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStageExecutionRepository extends JpaRepository<WorkflowStageExecution, Long> {
  List<WorkflowStageExecution> findByWorkflowRun_IdOrderByStageOrderAsc(Long workflowRunId);
  Optional<WorkflowStageExecution> findByWorkflowRun_IdAndStageKey(Long workflowRunId, String stageKey);
}
