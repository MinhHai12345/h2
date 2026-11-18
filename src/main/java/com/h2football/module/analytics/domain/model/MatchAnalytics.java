package com.h2football.module.analytics.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "match_analytics",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_match_analytics_source",
            columnNames = {"match_id", "provider"}))
public class MatchAnalytics {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MatchAnalyticsStatus status;

  @Column(name = "home_expected_goals", precision = 12, scale = 6)
  private BigDecimal homeExpectedGoals;

  @Column(name = "away_expected_goals", precision = 12, scale = 6)
  private BigDecimal awayExpectedGoals;

  @Column(name = "home_form_index", precision = 10, scale = 6)
  private BigDecimal homeFormIndex;

  @Column(name = "away_form_index", precision = 10, scale = 6)
  private BigDecimal awayFormIndex;

  @Column(name = "home_win_probability", precision = 10, scale = 8)
  private BigDecimal homeWinProbability;

  @Column(name = "draw_probability", precision = 10, scale = 8)
  private BigDecimal drawProbability;

  @Column(name = "away_win_probability", precision = 10, scale = 8)
  private BigDecimal awayWinProbability;

  @Column(name = "over_2_5_probability", precision = 10, scale = 8)
  private BigDecimal over25Probability;

  @Column(name = "both_teams_score_probability", precision = 10, scale = 8)
  private BigDecimal bothTeamsScoreProbability;

  @Column(name = "match_attractiveness_index", precision = 8, scale = 3)
  private BigDecimal matchAttractivenessIndex;

  @Column(name = "home_history_matches", nullable = false)
  private int homeHistoryMatches;

  @Column(name = "away_history_matches", nullable = false)
  private int awayHistoryMatches;

  @Column(name = "model_version", nullable = false, length = 40)
  private String modelVersion;

  @Column(name = "insufficient_data_reason", length = 500)
  private String insufficientDataReason;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;

  protected MatchAnalytics() {}

  public MatchAnalytics(
      Match match,
      Provider provider,
      MatchAnalyticsStatus status,
      BigDecimal homeExpectedGoals,
      BigDecimal awayExpectedGoals,
      BigDecimal homeFormIndex,
      BigDecimal awayFormIndex,
      BigDecimal homeWinProbability,
      BigDecimal drawProbability,
      BigDecimal awayWinProbability,
      BigDecimal over25Probability,
      BigDecimal bothTeamsScoreProbability,
      BigDecimal matchAttractivenessIndex,
      int homeHistoryMatches,
      int awayHistoryMatches,
      String modelVersion,
      String insufficientDataReason,
      Instant computedAt) {
    if (match == null || provider == null || status == null || computedAt == null) {
      throw new IllegalArgumentException("Match, provider, status and computedAt are required");
    }
    this.match = match;
    this.provider = provider;
    updateSnapshot(
        status,
        homeExpectedGoals,
        awayExpectedGoals,
        homeFormIndex,
        awayFormIndex,
        homeWinProbability,
        drawProbability,
        awayWinProbability,
        over25Probability,
        bothTeamsScoreProbability,
        matchAttractivenessIndex,
        homeHistoryMatches,
        awayHistoryMatches,
        modelVersion,
        insufficientDataReason,
        computedAt);
  }

  public void updateSnapshot(
      MatchAnalyticsStatus status,
      BigDecimal homeExpectedGoals,
      BigDecimal awayExpectedGoals,
      BigDecimal homeFormIndex,
      BigDecimal awayFormIndex,
      BigDecimal homeWinProbability,
      BigDecimal drawProbability,
      BigDecimal awayWinProbability,
      BigDecimal over25Probability,
      BigDecimal bothTeamsScoreProbability,
      BigDecimal matchAttractivenessIndex,
      int homeHistoryMatches,
      int awayHistoryMatches,
      String modelVersion,
      String insufficientDataReason,
      Instant computedAt) {
    if (status == null || computedAt == null) {
      throw new IllegalArgumentException("Status and computedAt are required");
    }
    if (homeHistoryMatches < 0 || awayHistoryMatches < 0) {
      throw new IllegalArgumentException("History match counts must not be negative");
    }
    if (modelVersion == null || modelVersion.isBlank() || modelVersion.length() > 40) {
      throw new IllegalArgumentException("modelVersion is invalid");
    }
    this.status = status;
    this.homeExpectedGoals = homeExpectedGoals;
    this.awayExpectedGoals = awayExpectedGoals;
    this.homeFormIndex = homeFormIndex;
    this.awayFormIndex = awayFormIndex;
    this.homeWinProbability = homeWinProbability;
    this.drawProbability = drawProbability;
    this.awayWinProbability = awayWinProbability;
    this.over25Probability = over25Probability;
    this.bothTeamsScoreProbability = bothTeamsScoreProbability;
    this.matchAttractivenessIndex = matchAttractivenessIndex;
    this.homeHistoryMatches = homeHistoryMatches;
    this.awayHistoryMatches = awayHistoryMatches;
    this.modelVersion = modelVersion.trim();
    this.insufficientDataReason =
        insufficientDataReason == null || insufficientDataReason.isBlank()
            ? null
            : insufficientDataReason.trim();
    this.computedAt = computedAt;
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

  public MatchAnalyticsStatus getStatus() {
    return status;
  }

  public BigDecimal getHomeExpectedGoals() {
    return homeExpectedGoals;
  }

  public BigDecimal getAwayExpectedGoals() {
    return awayExpectedGoals;
  }

  public BigDecimal getHomeFormIndex() {
    return homeFormIndex;
  }

  public BigDecimal getAwayFormIndex() {
    return awayFormIndex;
  }

  public BigDecimal getHomeWinProbability() {
    return homeWinProbability;
  }

  public BigDecimal getDrawProbability() {
    return drawProbability;
  }

  public BigDecimal getAwayWinProbability() {
    return awayWinProbability;
  }

  public BigDecimal getOver25Probability() {
    return over25Probability;
  }

  public BigDecimal getBothTeamsScoreProbability() {
    return bothTeamsScoreProbability;
  }

  public BigDecimal getMatchAttractivenessIndex() {
    return matchAttractivenessIndex;
  }

  public int getHomeHistoryMatches() {
    return homeHistoryMatches;
  }

  public int getAwayHistoryMatches() {
    return awayHistoryMatches;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public String getInsufficientDataReason() {
    return insufficientDataReason;
  }

  public Instant getComputedAt() {
    return computedAt;
  }
}
