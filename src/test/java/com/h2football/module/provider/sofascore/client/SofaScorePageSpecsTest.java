package com.h2football.module.provider.sofascore.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import org.junit.jupiter.api.Test;

class SofaScorePageSpecsTest {
  @Test
  void bindsCataloguePageToItsStaticAndDynamicEndpoints() {
    var spec = SofaScorePageSpecs.leagueCatalogue("https://www.sofascore.com/", "vn");
    assertThat(spec.loadPageUrl()).isEqualTo("https://www.sofascore.com/football");
    assertThat(
            spec.requiredEndpoint()
                .matches(
                    "https://www.sofascore.com/api/v1/config/top-unique-tournaments/VN/football"))
        .isTrue();
    var pattern =
        spec.endpoints().stream().filter(SofaScoreEndpointSpec::dynamic).findFirst().orElseThrow();
    assertThat(
            pattern.captureLong("https://www.sofascore.com/api/v1/unique-tournament/17/seasons", 1))
        .isEqualTo(17L);
  }

  @Test
  void bindsEventPageToRequestedWindowAndPage() {
    var spec =
        SofaScorePageSpecs.events(
            "https://www.sofascore.com", 17, 52186, SofaScoreEventWindow.NEXT, 2);
    assertThat(spec.loadPageUrl())
        .isEqualTo(
            "https://www.sofascore.com/football/unique-tournament/17/season/52186/matches/overall");
    assertThat(
            spec.requiredEndpoint()
                .matches(
                    "https://www.sofascore.com/api/v1/unique-tournament/17/season/52186/events/next/2"))
        .isTrue();
    assertThat(
            spec.requiredEndpoint()
                .matches(
                    "https://www.sofascore.com/api/v1/unique-tournament/17/season/52186/events/last/2"))
        .isFalse();
  }

  @Test
  void createsEventDetailEndpointPaths() {
    assertThat(SofaScorePageSpecs.eventStatisticsEndpoint(123L))
        .isEqualTo("/api/v1/event/123/statistics");
    assertThat(SofaScorePageSpecs.eventLineupsEndpoint(123L))
        .isEqualTo("/api/v1/event/123/lineups");
    assertThat(SofaScorePageSpecs.eventIncidentsEndpoint(123L))
        .isEqualTo("/api/v1/event/123/incidents");
  }

  @Test
  void rejectsInvalidTournamentOrSeasonIdsAndNegativePage() {
    assertThatThrownBy(() -> SofaScorePageSpecs.standings("https://www.sofascore.com", 0, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SofaScorePageSpecs.events(
                    "https://www.sofascore.com", 17, 10, SofaScoreEventWindow.LAST, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SofaScorePageSpecs.eventIncidentsEndpoint(0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
