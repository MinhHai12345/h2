package com.h2football.module.ingestion.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.provider.MultiProviderSeasonDataClient;
import org.springframework.stereotype.Service;

@Service
public class MultiProviderSeasonIngestionService {
  private final MultiProviderSeasonDataClient dataClient;
  private final ProviderSeasonPersistenceService persistence;

  public MultiProviderSeasonIngestionService(
      MultiProviderSeasonDataClient dataClient, ProviderSeasonPersistenceService persistence) {
    this.dataClient = dataClient;
    this.persistence = persistence;
  }

  public ProviderSeasonPersistenceService.ProviderSeasonSyncResult sync(
      Provider provider, long canonicalSeasonId, String externalLeagueId, int seasonYear) {
    if (provider == null || provider == Provider.SOFASCORE) {
      throw new IllegalArgumentException("A supported non-SofaScore provider is required");
    }
    var records = dataClient.fetch(provider, externalLeagueId, seasonYear);
    return persistence.persist(provider, canonicalSeasonId, records);
  }
}
