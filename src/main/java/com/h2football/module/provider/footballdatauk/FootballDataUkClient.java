package com.h2football.module.provider.footballdatauk;

import com.h2football.module.provider.ProviderIngestionProperties;
import com.h2football.module.provider.http.ProviderHttpClient;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class FootballDataUkClient {
  private static final Duration TIMEOUT = Duration.ofSeconds(45);
  private static final Charset SOURCE_CHARSET = Charset.forName("windows-1252");
  private final ProviderHttpClient http;
  private final ProviderIngestionProperties properties;

  public FootballDataUkClient(ProviderHttpClient http, ProviderIngestionProperties properties) {
    this.http = http;
    this.properties = properties;
  }

  public String fetchSeasonCsv(String leagueCode, int seasonYear) {
    var config = properties.getFootballDataUk();
    if (!config.isEnabled()) throw new IllegalStateException("Football-Data UK provider is disabled");
    if (leagueCode == null || !leagueCode.matches("[A-Za-z0-9]{1,8}")) {
      throw new IllegalArgumentException("A valid Football-Data.co.uk league code is required");
    }
    if (seasonYear < 1900 || seasonYear > 9998) {
      throw new IllegalArgumentException("A valid season start year is required");
    }
    String seasonCode = String.format(Locale.ROOT, "%02d%02d",
        seasonYear % 100, (seasonYear + 1) % 100);
    byte[] content = http.getBytes(config.getBaseUrl(), "/mmz4281/{season}/{league}.csv",
        Map.of(HttpHeaders.ACCEPT, "text/csv, text/plain, */*",
            HttpHeaders.USER_AGENT, "H2-Football-Data-Importer/1.0"),
        TIMEOUT, seasonCode, leagueCode.toUpperCase(Locale.ROOT));
    return content == null ? null : new String(content, SOURCE_CHARSET).replace("\uFEFF", "");
  }
}
