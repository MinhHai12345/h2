package com.h2football.module.ingestion.scheduling;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for recurring SofaScore season synchronization jobs. */
@ConfigurationProperties(prefix = "app.ingestion.sofascore.job")
public class SofaScoreIngestionJobProperties {
  private boolean enabled;
  private String cron = "0 15 2 * * ?";
  private List<Season> seasons = new ArrayList<>();

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

  public List<Season> getSeasons() {
    return seasons;
  }

  public void setSeasons(List<Season> seasons) {
    this.seasons = seasons == null ? new ArrayList<>() : new ArrayList<>(seasons);
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
