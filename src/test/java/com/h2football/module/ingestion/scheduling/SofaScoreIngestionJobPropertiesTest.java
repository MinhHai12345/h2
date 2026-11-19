package com.h2football.module.ingestion.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SofaScoreIngestionJobPropertiesTest {

  @Test
  void shouldProvideIndependentDefaultsForCatalogueAndSeasonJobs() {
    SofaScoreIngestionJobProperties properties = new SofaScoreIngestionJobProperties();

    assertThat(properties.getCron()).isEqualTo("0 15 2 * * ?");
    assertThat(properties.isRunOnStartup()).isFalse();
    assertThat(properties.getSeasons()).isEmpty();
    assertThat(properties.getLeagueCatalogue().isEnabled()).isFalse();
    assertThat(properties.getLeagueCatalogue().getCron()).isEqualTo("0 0 2 * * ?");
    assertThat(properties.getLeagueCatalogue().isRunOnStartup()).isFalse();
  }

  @Test
  void shouldDefensivelyCopyConfiguredTournamentIds() {
    SofaScoreIngestionJobProperties properties = new SofaScoreIngestionJobProperties();
    List<Long> ids = new java.util.ArrayList<>(List.of(17L, 8L));

    properties.getLeagueCatalogue().setTournamentIds(ids);
    ids.add(23L);

    assertThat(properties.getLeagueCatalogue().getTournamentIds()).containsExactly(17L, 8L);
  }
}
