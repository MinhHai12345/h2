package com.h2football.module.workflow.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WorkflowRunTest {
  @Test
  void leasePreventsConcurrentExecutionAndCanBeReclaimedAfterExpiry() {
    Instant now = Instant.parse("2026-11-19T06:00:00Z");
    WorkflowRun run = new WorkflowRun("SOFASCORE_SEASON_SYNC", "season-42", "{}", now);

    run.claim("worker-a", now.plusSeconds(60), now);
    assertEquals(WorkflowRunStatus.RUNNING, run.getStatus());

    assertThrows(
        IllegalStateException.class,
        () -> run.claim("worker-b", now.plusSeconds(120), now.plusSeconds(30)));

    run.claim("worker-b", now.plusSeconds(180), now.plusSeconds(61));
    assertEquals("worker-b", run.getLeaseOwner());
  }

  @Test
  void completionReleasesLeaseAndPreventsAnotherClaim() {
    Instant now = Instant.parse("2026-11-19T06:00:00Z");
    WorkflowRun run = new WorkflowRun("SOFASCORE_SEASON_SYNC", "season-42", "{}", now);
    run.claim("worker-a", now.plusSeconds(60), now);
    run.complete(now.plusSeconds(10));

    assertEquals(WorkflowRunStatus.SUCCEEDED, run.getStatus());
    assertNull(run.getLeaseOwner());
    assertNull(run.getLeaseExpiresAt());
    assertThrows(
        IllegalStateException.class,
        () -> run.claim("worker-b", now.plusSeconds(120), now.plusSeconds(11)));
  }
}
