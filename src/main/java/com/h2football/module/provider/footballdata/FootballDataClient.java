package com.h2football.module.provider.footballdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.h2football.module.provider.ProviderIngestionProperties;
import com.h2football.module.provider.http.ProviderHttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FootballDataClient {
  private static final Duration TIMEOUT = Duration.ofSeconds(60);
  private static final long MIN_REQUEST_INTERVAL_MILLIS = 6_000;
  private final ProviderHttpClient http;
  private final ProviderIngestionProperties properties;
  private long nextAllowedRequestAtMillis;

  public FootballDataClient(ProviderHttpClient http, ProviderIngestionProperties properties) {
    this.http = http;
    this.properties = properties;
  }

  public synchronized JsonNode fetchCompetitionMatches(String competitionCode, int seasonYear) {
    var config = properties.getFootballData();
    if (!config.isEnabled()) throw new IllegalStateException("Football-Data provider is disabled");
    if (!StringUtils.hasText(config.getApiKey())) {
      throw new IllegalStateException("FOOTBALL_DATA_API_KEY is required");
    }
    if (!StringUtils.hasText(competitionCode) || competitionCode.contains("/")) {
      throw new IllegalArgumentException("A valid football-data.org competition code is required");
    }
    awaitRequestSlot();
    try {
      return http.getJson(config.getBaseUrl(), "/competitions/{code}/matches?season={season}",
          Map.of("X-Auth-Token", config.getApiKey(), HttpHeaders.ACCEPT, "application/json"),
          JsonNode.class, TIMEOUT, competitionCode, seasonYear);
    } finally {
      nextAllowedRequestAtMillis = System.currentTimeMillis() + MIN_REQUEST_INTERVAL_MILLIS;
    }
  }

  private void awaitRequestSlot() {
    long waitMillis = nextAllowedRequestAtMillis - System.currentTimeMillis();
    if (waitMillis <= 0) return;
    try {
      Thread.sleep(waitMillis);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for football-data.org request slot", exception);
    }
  }
}
