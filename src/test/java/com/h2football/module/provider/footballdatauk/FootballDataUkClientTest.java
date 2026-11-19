package com.h2football.module.provider.footballdatauk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.provider.ProviderIngestionProperties;
import com.h2football.module.provider.http.ProviderHttpClient;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

class FootballDataUkClientTest {
  @Test
  void downloadsCsvUsingSourceCharacterSet() {
    ProviderHttpClient http = Mockito.mock(ProviderHttpClient.class);
    ProviderIngestionProperties properties = new ProviderIngestionProperties();
    properties.getFootballDataUk().setEnabled(true);
    FootballDataUkClient client = new FootballDataUkClient(http, properties);
    byte[] content = "Date,HomeTeam,AwayTeam\r\n01/08/24,Arsenal,Chelsea"
        .getBytes(Charset.forName("windows-1252"));
    when(http.getBytes(properties.getFootballDataUk().getBaseUrl(),
        "/mmz4281/{season}/{league}.csv",
        Map.of(HttpHeaders.ACCEPT, "text/csv, text/plain, */*",
            HttpHeaders.USER_AGENT, "H2-Football-Data-Importer/1.0"),
        Duration.ofSeconds(45), "2425", "E0")).thenReturn(content);

    assertEquals("Date,HomeTeam,AwayTeam\r\n01/08/24,Arsenal,Chelsea",
        client.fetchSeasonCsv("e0", 2024));
    verify(http).getBytes(properties.getFootballDataUk().getBaseUrl(),
        "/mmz4281/{season}/{league}.csv",
        Map.of(HttpHeaders.ACCEPT, "text/csv, text/plain, */*",
            HttpHeaders.USER_AGENT, "H2-Football-Data-Importer/1.0"),
        Duration.ofSeconds(45), "2425", "E0");
  }
}
