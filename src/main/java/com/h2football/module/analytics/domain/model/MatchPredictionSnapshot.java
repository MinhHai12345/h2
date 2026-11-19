package com.h2football.module.analytics.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "match_prediction_snapshots",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_prediction_snapshot_version_time",
            columnNames = {"match_id", "provider", "model_version", "computed_at"}),
    indexes = {
      @Index(
          name = "idx_prediction_snapshot_match_provider",
          columnList = "match_id, provider, computed_at"),
      @Index(
          name = "idx_prediction_snapshot_model_version",
          columnList = "model_version, evaluated_at")
    })
public class MatchPredictionSnapshot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Column(name = "model_version", nullable = false, length = 40)
  private String modelVersion;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;

  @Column(name = "home_win_probability", nullable = false, precision = 10, scale = 8)
  private BigDecimal homeWinProbability;

  @Column(name = "draw_probability", nullable = false, precision = 10, scale = 8)
  private BigDecimal drawProbability;

  @Column(name = "away_win_probability", nullable = false, precision = 10, scale = 8)
  private BigDecimal awayWinProbability;

  @Column(name = "actual_home_score")
  private Integer actualHomeScore;

  @Column(name = "actual_away_score")
  private Integer actualAwayScore;

  @Column(name = "actual_outcome", length = 10)
  private String actualOutcome;

  @Column(name = "brier_score", precision = 12, scale = 10)
  private BigDecimal brierScore;

  @Column(name = "log_loss", precision = 12, scale = 10)
  private BigDecimal logLoss;

  @Column(name = "evaluated_at")
  private Instant evaluatedAt;

  protected MatchPredictionSnapshot() {}

  public MatchPredictionSnapshot(
      Match match,
      Provider provider,
      String modelVersion,
      Instant computedAt,
      BigDecimal homeWinProbability,
      BigDecimal drawProbability,
      BigDecimal awayWinProbability) {
    if (match == null || provider == null || computedAt == null) {
      throw new IllegalArgumentException("Match, provider and computedAt are required");
    }
    if (modelVersion == null || modelVersion.isBlank() || modelVersion.length() > 40) {
      throw new IllegalArgumentException("modelVersion is invalid");
    }
    validateProbability(homeWinProbability, "homeWinProbability");
    validateProbability(drawProbability, "drawProbability");
    validateProbability(awayWinProbability, "awayWinProbability");
    BigDecimal total = homeWinProbability.add(drawProbability).add(awayWinProbability);
    if (total.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.000001")) > 0) {
      throw new IllegalArgumentException("Outcome probabilities must sum to 1");
    }
    this.match = match;
    this.provider = provider;
    this.modelVersion = modelVersion.trim();
    this.computedAt = computedAt;
    this.homeWinProbability = homeWinProbability;
    this.drawProbability = drawProbability;
    this.awayWinProbability = awayWinProbability;
  }

  public void evaluate(
      int homeScore,
      int awayScore,
      BigDecimal brierScore,
      BigDecimal logLoss,
      Instant evaluatedAt) {
    if (homeScore < 0
        || awayScore < 0
        || brierScore == null
        || logLoss == null
        || evaluatedAt == null) {
      throw new IllegalArgumentException("Valid result scores and evaluation metrics are required");
    }
    if (brierScore.signum() < 0 || logLoss.signum() < 0) {
      throw new IllegalArgumentException("Evaluation metrics must not be negative");
    }
    this.actualHomeScore = homeScore;
    this.actualAwayScore = awayScore;
    this.actualOutcome = homeScore > awayScore ? "HOME" : homeScore == awayScore ? "DRAW" : "AWAY";
    this.brierScore = brierScore;
    this.logLoss = logLoss;
    this.evaluatedAt = evaluatedAt;
  }

  public boolean hasResult(int homeScore, int awayScore) {
    return actualHomeScore != null
        && actualAwayScore != null
        && actualHomeScore == homeScore
        && actualAwayScore == awayScore
        && evaluatedAt != null;
  }

  private void validateProbability(BigDecimal value, String field) {
    if (value == null
        || value.compareTo(BigDecimal.ZERO) < 0
        || value.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException(field + " must be between 0 and 1");
    }
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

  public String getModelVersion() {
    return modelVersion;
  }

  public Instant getComputedAt() {
    return computedAt;
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

  public Integer getActualHomeScore() {
    return actualHomeScore;
  }

  public Integer getActualAwayScore() {
    return actualAwayScore;
  }

  public String getActualOutcome() {
    return actualOutcome;
  }

  public BigDecimal getBrierScore() {
    return brierScore;
  }

  public BigDecimal getLogLoss() {
    return logLoss;
  }

  public Instant getEvaluatedAt() {
    return evaluatedAt;
  }

  public boolean isEvaluated() {
    return evaluatedAt != null;
  }
}
