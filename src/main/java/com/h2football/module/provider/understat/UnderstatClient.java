package com.h2football.module.provider.understat;

import com.fasterxml.jackson.databind.JsonNode;
import com.h2football.module.provider.ProviderIngestionProperties;
import com.h2football.module.provider.http.ProviderHttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UnderstatClient {
  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private final ProviderHttpClient http;
  private final ProviderIngestionProperties properties;

  public UnderstatClient(ProviderHttpClient http, ProviderIngestionProperties properties) {
    this.http = http;
    this.properties = properties;
  }

  public JsonNode fetchLeagueData(String leagueSlug, int seasonYear) {
    var config = properties.getUnderstat();
    if (!config.isEnabled()) {
      throw new IllegalStateException("Understat provider is disabled");
    }
    if (!StringUtils.hasText(leagueSlug) || leagueSlug.contains("/") || leagueSlug.contains("..")) {
      throw new IllegalArgumentException("A valid Understat league slug is required");
    }
    if (seasonYear < 2014) {
      throw new IllegalArgumentException("Understat season must be 2014 or later");
    }
    return http.getJson(
        config.getBaseUrl(),
        "/getLeagueData/{league}/{season}",
        Map.of(
            HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01",
            HttpHeaders.REFERER, config.getBaseUrl() + "/league/" + leagueSlug + "/" + seasonYear,
            "X-Requested-With", "XMLHttpRequest"),
        JsonNode.class,
        TIMEOUT,
        leagueSlug,
        seasonYear);
  }
}
