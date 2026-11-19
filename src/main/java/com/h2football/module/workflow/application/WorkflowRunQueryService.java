package com.h2football.module.workflow.application;

import com.h2football.module.workflow.domain.model.WorkflowRun;
import com.h2football.module.workflow.domain.model.WorkflowStageExecution;
import com.h2football.module.workflow.domain.repository.WorkflowRunRepository;
import com.h2football.module.workflow.domain.repository.WorkflowStageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkflowRunQueryService {
  private final WorkflowRunRepository runs;
  private final WorkflowStageExecutionRepository stages;

  public WorkflowRunQueryService(
      WorkflowRunRepository runs, WorkflowStageExecutionRepository stages) {
    this.runs = runs;
    this.stages = stages;
  }

  public WorkflowRun getRun(Long id) {
    return runs.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Workflow run not found: " + id));
  }

  public List<WorkflowStageExecution> findStages(Long runId) {
    return stages.findByWorkflowRun_IdOrderByStageOrderAsc(runId);
  }
}
