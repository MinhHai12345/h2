package com.h2football.module.provider.sofascore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import org.junit.jupiter.api.Test;

class SofaScoreMasterDataMapperTest {
  private final SofaScoreMasterDataMapper mapper = new SofaScoreMasterDataMapper();

  @Test
  void mapsSeasonLabelsToCanonicalStartYear() {
    assertThat(SofaScoreMasterDataMapper.canonicalStartYear("2025/2026")).isEqualTo("2025");
    assertThat(SofaScoreMasterDataMapper.canonicalStartYear("26/27")).isEqualTo("2026");
    assertThat(SofaScoreMasterDataMapper.canonicalStartYear("2024")).isEqualTo("2024");
    assertThat(SofaScoreMasterDataMapper.canonicalStartYear("unknown")).isNull();
  }

  @Test
  void mapsTournamentAndKeepsCountryCodeCanonical() {
    var tournament =
        new SofaScoreDtos.Tournament(
            17L,
            "Premier League",
            "premier-league",
            new SofaScoreDtos.Category(1L, "England", "gb"));
    var snapshot =
        mapper.toLeagueSnapshot(
            tournament,
            java.util.List.of(new SofaScoreDtos.Season(101L, "2025/2026", "2025", true)));

    assertThat(snapshot.externalId()).isEqualTo("17");
    assertThat(snapshot.countryIsoCode()).isEqualTo("GB");
    assertThat(snapshot.seasons()).hasSize(1);
    assertThat(snapshot.seasons().get(0).seasonCode()).isEqualTo("2025");
  }

  @Test
  void rejectsSeasonWithoutUsableYear() {
    assertThat(mapper.toSeasonSnapshot(new SofaScoreDtos.Season(1L, "Unknown", null, false)))
        .isNull();
  }
}
