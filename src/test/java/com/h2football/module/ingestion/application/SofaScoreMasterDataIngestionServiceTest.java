package com.h2football.module.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.application.MasterDataImportApplicationService;
import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.provider.sofascore.SofaScoreMasterDataProvider;
import com.h2football.module.provider.sofascore.model.SofaScoreLeagueSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreSeasonSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class SofaScoreMasterDataIngestionServiceTest {
  private final SofaScoreMasterDataProvider provider = mock(SofaScoreMasterDataProvider.class);
  private final MasterDataImportApplicationService masterData =
      mock(MasterDataImportApplicationService.class);
  private final SofaScoreMasterDataIngestionService service =
      new SofaScoreMasterDataIngestionService(provider, masterData);

  @Test
  void importsLeagueAndReturnsSeasonDiscoveryWithoutInventingDates() {
    Country country = mock(Country.class);
    when(country.getId()).thenReturn(4L);
    when(provider.fetchLeagues(List.of(17L)))
        .thenReturn(
            List.of(
                new SofaScoreLeagueSnapshot(
                    "17",
                    "Premier League",
                    "GB",
                    "England",
                    List.of(new SofaScoreSeasonSnapshot("52186", "2025", "2025/2026", true)))));
    when(masterData.importCountry(Provider.SOFASCORE, "GB", "GB", "England")).thenReturn(country);

    var result = service.syncLeagues(List.of(17L));

    assertThat(result.importedLeagues()).isEqualTo(1);
    assertThat(result.discoveredSeasons()).isEqualTo(1);
    assertThat(result.skippedSeasons()).isEqualTo(1);
    assertThat(result.leagues().get(0).seasons().get(0).externalId()).isEqualTo("52186");
    assertThat(result.warnings()).anyMatch(message -> message.contains("startDate/endDate"));
    verify(masterData).importLeague(Provider.SOFASCORE, "17", "SOFASCORE_17", "Premier League", 4L);
  }
}
