package com.h2football.module.provider.sofascore.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SofaScorePageSpecsTest {
    @Test
    void bindsCataloguePageToItsStaticAndDynamicEndpoints() {
        var spec = SofaScorePageSpecs.leagueCatalogue("https://www.sofascore.com/", "vn");
        assertThat(spec.loadPageUrl()).isEqualTo("https://www.sofascore.com/football");
        assertThat(spec.requiredEndpoint().matches(
                "https://www.sofascore.com/api/v1/config/top-unique-tournaments/VN/football")).isTrue();
        var pattern = spec.endpoints().stream().filter(SofaScoreEndpointSpec::dynamic).findFirst().orElseThrow();
        assertThat(pattern.captureLong(
                "https://www.sofascore.com/api/v1/unique-tournament/17/seasons", 1)).isEqualTo(17L);
    }

    @Test
    void rejectsInvalidTournamentOrSeasonIds() {
        assertThatThrownBy(() -> SofaScorePageSpecs.standings("https://www.sofascore.com", 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
