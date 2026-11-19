package com.h2football.module.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.h2football.module.identity.domain.model.Provider;
import org.junit.jupiter.api.Test;

class ProviderSeasonWorkflowRequestTest {
  @Test
  void shouldNormalizeExternalLeagueIdentifier() {
    var request =
        new ProviderSeasonWorkflowRequest(
            "fd-pl-2024", Provider.FOOTBALL_DATA, 10L, " PL ", 2024);

    assertThat(request.externalLeagueId()).isEqualTo("PL");
  }

  @Test
  void shouldRejectSofaScoreBecauseItHasItsOwnIngestionPipeline() {
    assertThatThrownBy(
            () ->
                new ProviderSeasonWorkflowRequest(
                    "sofa-2024", Provider.SOFASCORE, 10L, "17", 2024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SofaScore ingestion");
  }

  @Test
  void shouldRejectUnderstatSeasonsBeforeItsSupportedHistoricalRange() {
    assertThatThrownBy(
            () ->
                new ProviderSeasonWorkflowRequest(
                    "understat-2010", Provider.UNDERSTAT, 10L, "epl", 2010))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2014");
  }
}
