package com.h2football.module.analytics.application;

import com.h2football.module.identity.domain.model.Provider;

public class MatchAnalyticsNotFoundException extends RuntimeException {
  public MatchAnalyticsNotFoundException(Long matchId, Provider provider) {
    super("Match analytics not found for match " + matchId + " and provider " + provider);
  }
}
