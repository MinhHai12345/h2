package com.h2football.module.analytics.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "team_season_statistics",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_team_season_statistics_source",
            columnNames = {"season_id", "team_id", "provider", "period", "statistic_key"}))
public class TeamSeasonStatistic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Column(nullable = false, length = 20)
  private String period;

  @Column(name = "group_name", nullable = false, length = 80)
  private String groupName;

  @Column(name = "statistic_key", nullable = false, length = 100)
  private String statisticKey;

  @Column(name = "statistic_name", nullable = false, length = 120)
  private String statisticName;

  @Column(name = "matches_count", nullable = false)
  private Long matchesCount;

  @Column(name = "total_value", precision = 18, scale = 4)
  private BigDecimal totalValue;

  @Column(name = "average_value", precision = 18, scale = 4)
  private BigDecimal averageValue;

  protected TeamSeasonStatistic() {}

  public TeamSeasonStatistic(
      Season season,
      Team team,
      Provider provider,
      String period,
      String groupName,
      String statisticKey,
      String statisticName,
      Long matchesCount,
      BigDecimal totalValue,
      BigDecimal averageValue) {
    if (season == null || team == null || provider == null) {
      throw new IllegalArgumentException("Season, team and provider are required");
    }
    this.season = season;
    this.team = team;
    this.provider = provider;
    this.period = required(period, "period", 20);
    this.groupName = required(groupName, "groupName", 80);
    this.statisticKey = required(statisticKey, "statisticKey", 100);
    this.statisticName = required(statisticName, "statisticName", 120);
    if (matchesCount == null || matchesCount < 0) {
      throw new IllegalArgumentException("matchesCount must not be negative or null");
    }
    this.matchesCount = matchesCount;
    this.totalValue = totalValue;
    this.averageValue = averageValue;
  }

  private static String required(String value, String field, int maxLength) {
    if (value == null || value.isBlank() || value.trim().length() > maxLength) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return value.trim();
  }

  public Long getId() {
    return id;
  }

  public Season getSeason() {
    return season;
  }

  public Team getTeam() {
    return team;
  }

  public Provider getProvider() {
    return provider;
  }

  public String getPeriod() {
    return period;
  }

  public String getGroupName() {
    return groupName;
  }

  public String getStatisticKey() {
    return statisticKey;
  }

  public String getStatisticName() {
    return statisticName;
  }

  public Long getMatchesCount() {
    return matchesCount;
  }

  public BigDecimal getTotalValue() {
    return totalValue;
  }

  public BigDecimal getAverageValue() {
    return averageValue;
  }
}
