package com.h2football.module.provider.standardization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProviderDataStandardizerTest {
  @Test
  void shouldNormalizeLeagueAliasesToCanonicalNames() {
    assertThat(ProviderDataStandardizer.leagueName("Primera División")).isEqualTo("LaLiga");
    assertThat(ProviderDataStandardizer.leagueName("English Premier League"))
        .isEqualTo("Premier League");
    assertThat(ProviderDataStandardizer.leagueName("Serie A")).isEqualTo("Serie A");
  }

  @Test
  void shouldNormalizeKnownTeamAliasesAcrossProviders() {
    assertThat(ProviderDataStandardizer.teamName("FC Bayern München")).isEqualTo("Bayern Munich");
    assertThat(ProviderDataStandardizer.teamName("Bayern Munich")).isEqualTo("Bayern Munich");
    assertThat(ProviderDataStandardizer.teamName("FC Cologne")).isEqualTo("FC Köln");
    assertThat(ProviderDataStandardizer.teamName("Inter Milan")).isEqualTo("Inter");
  }

  @Test
  void shouldPreserveUnknownNamesAndUseConservativeMatchKeys() {
    assertThat(ProviderDataStandardizer.teamName("  Unknown FC  ")).isEqualTo("Unknown FC");
    assertThat(ProviderDataStandardizer.teamMatchKey("FC Köln"))
        .isEqualTo(ProviderDataStandardizer.teamMatchKey("FC Koln"));
    assertThat(ProviderDataStandardizer.teamMatchKey("United"))
        .isNotEqualTo(ProviderDataStandardizer.teamMatchKey("Manchester United"));
  }

  @Test
  void shouldReturnNullForBlankNames() {
    assertThat(ProviderDataStandardizer.leagueName(" ")).isNull();
    assertThat(ProviderDataStandardizer.teamName(null)).isNull();
    assertThat(ProviderDataStandardizer.teamMatchKey(null)).isEmpty();
  }
}
