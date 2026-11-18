package com.h2football.module.match.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "match_statistics",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_match_statistics_source",
            columnNames = {"match_id", "provider", "period", "statistic_key"}))
public class MatchStatistic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

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

  @Column(name = "home_display", length = 80)
  private String homeDisplay;

  @Column(name = "away_display", length = 80)
  private String awayDisplay;

  @Column(name = "home_value", precision = 14, scale = 4)
  private BigDecimal homeValue;

  @Column(name = "away_value", precision = 14, scale = 4)
  private BigDecimal awayValue;

  protected MatchStatistic() {}

  public MatchStatistic(
      Match match,
      Provider provider,
      String period,
      String groupName,
      String statisticKey,
      String statisticName,
      String homeDisplay,
      String awayDisplay,
      BigDecimal homeValue,
      BigDecimal awayValue) {
    if (match == null || provider == null)
      throw new IllegalArgumentException("match and provider are required");
    this.match = match;
    this.provider = provider;
    this.period = required(period, "period", 20);
    this.groupName = required(groupName, "groupName", 80);
    this.statisticKey = required(statisticKey, "statisticKey", 100);
    this.statisticName = required(statisticName, "statisticName", 120);
    this.homeDisplay = optional(homeDisplay, 80);
    this.awayDisplay = optional(awayDisplay, 80);
    this.homeValue = homeValue;
    this.awayValue = awayValue;
  }

  private static String required(String value, String field, int max) {
    String result = optional(value, max);
    if (result == null) throw new IllegalArgumentException(field + " is required");
    return result;
  }

  private static String optional(String value, int max) {
    if (value == null || value.isBlank()) return null;
    String result = value.trim();
    if (result.length() > max)
      throw new IllegalArgumentException("Statistic text exceeds " + max + " characters");
    return result;
  }

  public Long getId() {
    return id;
  }

  public Match getMatch() {
    return match;
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

  public String getHomeDisplay() {
    return homeDisplay;
  }

  public String getAwayDisplay() {
    return awayDisplay;
  }

  public BigDecimal getHomeValue() {
    return homeValue;
  }

  public BigDecimal getAwayValue() {
    return awayValue;
  }
}
