package com.h2football.module.provider;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.providers")
public class ProviderIngestionProperties {
  private final FootballData footballData = new FootballData();
  private final FootballDataUk footballDataUk = new FootballDataUk();
  private final Understat understat = new Understat();

  public FootballData getFootballData() {
    return footballData;
  }

  public FootballDataUk getFootballDataUk() {
    return footballDataUk;
  }

  public Understat getUnderstat() {
    return understat;
  }

  public static class ProviderConfig {
    private boolean enabled;
    private String baseUrl;
    private String cron = "0 45 2 * * ?";
    private boolean runOnStartup;
    private List<SeasonJob> seasons = new ArrayList<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getCron() {
      return cron;
    }

    public void setCron(String cron) {
      this.cron = cron;
    }

    public boolean isRunOnStartup() {
      return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
      this.runOnStartup = runOnStartup;
    }

    public List<SeasonJob> getSeasons() {
      return seasons;
    }

    public void setSeasons(List<SeasonJob> seasons) {
      this.seasons = seasons == null ? new ArrayList<>() : new ArrayList<>(seasons);
    }
  }

  public static class FootballData extends ProviderConfig {
    private String apiKey;

    public FootballData() {
      setBaseUrl("https://api.football-data.org/v4");
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }
  }

  public static class FootballDataUk extends ProviderConfig {
    public FootballDataUk() {
      setBaseUrl("https://football-data.co.uk");
    }
  }

  public static class Understat extends ProviderConfig {
    public Understat() {
      setBaseUrl("https://understat.com");
    }
  }

  public static class SeasonJob {
    private long canonicalSeasonId;
    private String externalLeagueId;
    private int seasonYear;

    public long getCanonicalSeasonId() {
      return canonicalSeasonId;
    }

    public void setCanonicalSeasonId(long canonicalSeasonId) {
      this.canonicalSeasonId = canonicalSeasonId;
    }

    public String getExternalLeagueId() {
      return externalLeagueId;
    }

    public void setExternalLeagueId(String externalLeagueId) {
      this.externalLeagueId = externalLeagueId;
    }

    public int getSeasonYear() {
      return seasonYear;
    }

    public void setSeasonYear(int seasonYear) {
      this.seasonYear = seasonYear;
    }
  }
}
