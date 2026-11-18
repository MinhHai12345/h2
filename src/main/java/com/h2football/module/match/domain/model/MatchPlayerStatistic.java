package com.h2football.module.match.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.player.domain.model.Player;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "match_player_statistics",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_match_player_statistics_source",
            columnNames = {"match_id", "player_id", "provider"}))
public class MatchPlayerStatistic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Column(precision = 8, scale = 3)
  private BigDecimal rating;

  @Column(name = "minutes_played")
  private Integer minutesPlayed;

  private Integer goals;
  private Integer assists;

  @Column(name = "total_shots")
  private Integer totalShots;

  @Column(name = "shots_on_target")
  private Integer shotsOnTarget;

  @Column(name = "accurate_passes")
  private Integer accuratePasses;

  @Column(name = "total_passes")
  private Integer totalPasses;

  @Column(name = "key_passes")
  private Integer keyPasses;

  private Integer tackles;
  private Integer interceptions;
  private Integer clearances;

  @Column(name = "expected_goals", precision = 12, scale = 4)
  private BigDecimal expectedGoals;

  @Column(name = "expected_assists", precision = 12, scale = 4)
  private BigDecimal expectedAssists;

  @Column(name = "raw_statistics", columnDefinition = "LONGTEXT")
  private String rawStatistics;

  protected MatchPlayerStatistic() {}

  public MatchPlayerStatistic(
      Match match, Player player, Provider provider, MatchPlayerStatisticSnapshot snapshot) {
    if (match == null || player == null || provider == null) {
      throw new IllegalArgumentException("Match, player and provider are required");
    }
    this.match = match;
    this.player = player;
    this.provider = provider;
    updateSnapshot(snapshot);
  }

  public void updateSnapshot(MatchPlayerStatisticSnapshot snapshot) {
    if (snapshot == null) {
      throw new IllegalArgumentException("Player statistic snapshot is required");
    }
    this.rating = snapshot.rating();
    this.minutesPlayed = nonNegative(snapshot.minutesPlayed(), "minutesPlayed");
    this.goals = nonNegative(snapshot.goals(), "goals");
    this.assists = nonNegative(snapshot.assists(), "assists");
    this.totalShots = nonNegative(snapshot.totalShots(), "totalShots");
    this.shotsOnTarget = nonNegative(snapshot.shotsOnTarget(), "shotsOnTarget");
    this.accuratePasses = nonNegative(snapshot.accuratePasses(), "accuratePasses");
    this.totalPasses = nonNegative(snapshot.totalPasses(), "totalPasses");
    this.keyPasses = nonNegative(snapshot.keyPasses(), "keyPasses");
    this.tackles = nonNegative(snapshot.tackles(), "tackles");
    this.interceptions = nonNegative(snapshot.interceptions(), "interceptions");
    this.clearances = nonNegative(snapshot.clearances(), "clearances");
    this.expectedGoals = snapshot.expectedGoals();
    this.expectedAssists = snapshot.expectedAssists();
    this.rawStatistics = snapshot.rawStatistics();
  }

  private Integer nonNegative(Integer value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
    return value;
  }

  public Long getId() {
    return id;
  }

  public Match getMatch() {
    return match;
  }

  public Player getPlayer() {
    return player;
  }

  public Provider getProvider() {
    return provider;
  }

  public BigDecimal getRating() {
    return rating;
  }

  public Integer getMinutesPlayed() {
    return minutesPlayed;
  }

  public Integer getGoals() {
    return goals;
  }

  public Integer getAssists() {
    return assists;
  }

  public Integer getTotalShots() {
    return totalShots;
  }

  public Integer getShotsOnTarget() {
    return shotsOnTarget;
  }

  public Integer getAccuratePasses() {
    return accuratePasses;
  }

  public Integer getTotalPasses() {
    return totalPasses;
  }

  public Integer getKeyPasses() {
    return keyPasses;
  }

  public Integer getTackles() {
    return tackles;
  }

  public Integer getInterceptions() {
    return interceptions;
  }

  public Integer getClearances() {
    return clearances;
  }

  public BigDecimal getExpectedGoals() {
    return expectedGoals;
  }

  public BigDecimal getExpectedAssists() {
    return expectedAssists;
  }

  public String getRawStatistics() {
    return rawStatistics;
  }

  public record MatchPlayerStatisticSnapshot(
      BigDecimal rating,
      Integer minutesPlayed,
      Integer goals,
      Integer assists,
      Integer totalShots,
      Integer shotsOnTarget,
      Integer accuratePasses,
      Integer totalPasses,
      Integer keyPasses,
      Integer tackles,
      Integer interceptions,
      Integer clearances,
      BigDecimal expectedGoals,
      BigDecimal expectedAssists,
      String rawStatistics) {}
}
