package com.h2football.module.ingestion.scheduling;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for recurring SofaScore ingestion jobs. */
@ConfigurationProperties(prefix = "app.ingestion.sofascore.job")
public class SofaScoreIngestionJobProperties {
  private boolean enabled;
  private String cron = "0 15 2 * * ?";
  private boolean runOnStartup;
  private List<Season> seasons = new ArrayList<>();
  private final LeagueCatalogue leagueCatalogue = new LeagueCatalogue();
  private final MatchDetails matchDetails = new MatchDetails();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
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

  public List<Season> getSeasons() {
    return seasons;
  }

  public void setSeasons(List<Season> seasons) {
    this.seasons = seasons == null ? new ArrayList<>() : new ArrayList<>(seasons);
  }

  public LeagueCatalogue getLeagueCatalogue() {
    return leagueCatalogue;
  }

  public MatchDetails getMatchDetails() {
    return matchDetails;
  }

  public static class LeagueCatalogue {
    private boolean enabled;
    private String cron = "0 0 2 * * ?";
    private boolean runOnStartup;
    private List<Long> tournamentIds = new ArrayList<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
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

    public List<Long> getTournamentIds() {
      return tournamentIds;
    }

    public void setTournamentIds(List<Long> tournamentIds) {
      this.tournamentIds =
          tournamentIds == null ? new ArrayList<>() : new ArrayList<>(tournamentIds);
    }
  }

  public static class MatchDetails {
    private boolean enabled;
    private int retryAttempts = 3;
    private long retryDelayMillis = 1000;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getRetryAttempts() {
      return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
      this.retryAttempts = retryAttempts;
    }

    public long getRetryDelayMillis() {
      return retryDelayMillis;
    }

    public void setRetryDelayMillis(long retryDelayMillis) {
      this.retryDelayMillis = retryDelayMillis;
    }
  }

  public static class Season {
    private long tournamentId;
    private long sofaSeasonId;
    private long canonicalSeasonId;

    public long getTournamentId() {
      return tournamentId;
    }

    public void setTournamentId(long tournamentId) {
      this.tournamentId = tournamentId;
    }

    public long getSofaSeasonId() {
      return sofaSeasonId;
    }

    public void setSofaSeasonId(long sofaSeasonId) {
      this.sofaSeasonId = sofaSeasonId;
    }

    public long getCanonicalSeasonId() {
      return canonicalSeasonId;
    }

    public void setCanonicalSeasonId(long canonicalSeasonId) {
      this.canonicalSeasonId = canonicalSeasonId;
    }
  }
}
