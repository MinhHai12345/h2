package com.h2football.module.workflow.application;

import com.h2football.module.identity.domain.model.Provider;

public record ProviderSeasonWorkflowRequest(
    String idempotencyKey,
    Provider provider,
    long canonicalSeasonId,
    String externalLeagueId,
    int seasonYear) {
  public ProviderSeasonWorkflowRequest {
    if (idempotencyKey == null
        || idempotencyKey.isBlank()
        || provider == null
        || canonicalSeasonId <= 0
        || externalLeagueId == null
        || externalLeagueId.isBlank()
        || seasonYear < 1900) {
      throw new IllegalArgumentException("Provider, external league, season and idempotency key are required");
    }
    if (provider == Provider.SOFASCORE) {
      throw new IllegalArgumentException("Use the SofaScore season workflow for SofaScore ingestion");
    }
    if (provider == Provider.UNDERSTAT && seasonYear < 2014) {
      throw new IllegalArgumentException("Understat supports seasons from 2014 onwards");
    }
    externalLeagueId = externalLeagueId.trim();
  }
}
