package com.h2football.module.provider.footballdata;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.h2football.module.provider.ProviderIngestionProperties;
import com.h2football.module.provider.http.ProviderHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FootballDataClientTest {
  @Test
  void requiresApiTokenBeforeCallingTransport() {
    ProviderHttpClient http = Mockito.mock(ProviderHttpClient.class);
    ProviderIngestionProperties properties = new ProviderIngestionProperties();
    properties.getFootballData().setEnabled(true);
    FootballDataClient client = new FootballDataClient(http, properties);

    assertThrows(IllegalStateException.class, () -> client.fetchCompetitionMatches("PL", 2024));
    Mockito.verifyNoInteractions(http);
  }
}
