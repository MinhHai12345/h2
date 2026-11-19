package com.h2football.module.provider.understat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.provider.ProviderIngestionProperties;
import com.h2football.module.provider.http.ProviderHttpClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

class UnderstatClientTest {
  @Test
  void delegatesLeagueRequestToSharedTransport() throws Exception {
    ProviderHttpClient http = Mockito.mock(ProviderHttpClient.class);
    ProviderIngestionProperties properties = new ProviderIngestionProperties();
    properties.getUnderstat().setEnabled(true);
    UnderstatClient client = new UnderstatClient(http, properties);
    JsonNode expected = new ObjectMapper().readTree("{\"dates\":[]}");
    when(http.getJson(properties.getUnderstat().getBaseUrl(), "/getLeagueData/{league}/{season}",
        Map.of(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01",
            HttpHeaders.REFERER, properties.getUnderstat().getBaseUrl() + "/league/epl/2024",
            "X-Requested-With", "XMLHttpRequest"),
        JsonNode.class, Duration.ofSeconds(30), "epl", 2024)).thenReturn(expected);

    client.fetchLeagueData("epl", 2024);
    verify(http).getJson(properties.getUnderstat().getBaseUrl(),
        "/getLeagueData/{league}/{season}",
        Map.of(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01",
            HttpHeaders.REFERER, properties.getUnderstat().getBaseUrl() + "/league/epl/2024",
            "X-Requested-With", "XMLHttpRequest"),
        JsonNode.class, Duration.ofSeconds(30), "epl", 2024);
  }

  @Test
  void validatesHistoricalSeason() {
    ProviderHttpClient http = Mockito.mock(ProviderHttpClient.class);
    ProviderIngestionProperties properties = new ProviderIngestionProperties();
    properties.getUnderstat().setEnabled(true);
    UnderstatClient client = new UnderstatClient(http, properties);
    assertThrows(IllegalArgumentException.class, () -> client.fetchLeagueData("epl", 2013));
  }
}
