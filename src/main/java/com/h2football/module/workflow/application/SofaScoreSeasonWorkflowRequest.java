package com.h2football.module.workflow.application;

public record SofaScoreSeasonWorkflowRequest(
    String idempotencyKey, long tournamentId, long sofaSeasonId, long canonicalSeasonId) {
  public SofaScoreSeasonWorkflowRequest {
    if (idempotencyKey == null
        || idempotencyKey.isBlank()
        || tournamentId <= 0
        || sofaSeasonId <= 0
        || canonicalSeasonId <= 0) {
      throw new IllegalArgumentException(
          "Idempotency key and positive season identifiers are required");
    }
  }
}
