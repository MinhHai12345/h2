package com.h2football.module.player.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Season;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "player_season_statistics",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_player_season_statistics_source",
            columnNames = {"season_id", "player_id", "provider"}))
public class PlayerSeasonStatistic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Column(nullable = false)
  private Long appearances;

  @Column(name = "minutes_played", nullable = false)
  private Long minutesPlayed;

  @Column(nullable = false)
  private Long goals;

  @Column(nullable = false)
  private Long assists;

  @Column(name = "total_shots", nullable = false)
  private Long totalShots;

  @Column(name = "shots_on_target", nullable = false)
  private Long shotsOnTarget;

  @Column(name = "accurate_passes", nullable = false)
  private Long accuratePasses;

  @Column(name = "total_passes", nullable = false)
  private Long totalPasses;

  @Column(name = "key_passes", nullable = false)
  private Long keyPasses;

  @Column(nullable = false)
  private Long tackles;

  @Column(nullable = false)
  private Long interceptions;

  @Column(nullable = false)
  private Long clearances;

  @Column(name = "expected_goals", precision = 14, scale = 4)
  private BigDecimal expectedGoals;

  @Column(name = "expected_assists", precision = 14, scale = 4)
  private BigDecimal expectedAssists;

  @Column(name = "average_rating", precision = 8, scale = 3)
  private BigDecimal averageRating;

  protected PlayerSeasonStatistic() {}

  public PlayerSeasonStatistic(
      Season season, Player player, Provider provider, PlayerSeasonStatisticSnapshot snapshot) {
    if (season == null || player == null || provider == null) {
      throw new IllegalArgumentException("Season, player and provider are required");
    }
    this.season = season;
    this.player = player;
    this.provider = provider;
    updateSnapshot(snapshot);
  }

  public void updateSnapshot(PlayerSeasonStatisticSnapshot snapshot) {
    if (snapshot == null) {
      throw new IllegalArgumentException("Season statistic snapshot is required");
    }
    this.appearances = nonNegative(snapshot.appearances(), "appearances");
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
    this.averageRating = snapshot.averageRating();
  }

  private Long nonNegative(Long value, String field) {
    if (value == null || value < 0) {
      throw new IllegalArgumentException(field + " must not be negative or null");
    }
    return value;
  }

  public Long getId() {
    return id;
  }

  public Season getSeason() {
    return season;
  }

  public Player getPlayer() {
    return player;
  }

  public Provider getProvider() {
    return provider;
  }

  public Long getAppearances() {
    return appearances;
  }

  public Long getMinutesPlayed() {
    return minutesPlayed;
  }

  public Long getGoals() {
    return goals;
  }

  public Long getAssists() {
    return assists;
  }

  public Long getTotalShots() {
    return totalShots;
  }

  public Long getShotsOnTarget() {
    return shotsOnTarget;
  }

  public Long getAccuratePasses() {
    return accuratePasses;
  }

  public Long getTotalPasses() {
    return totalPasses;
  }

  public Long getKeyPasses() {
    return keyPasses;
  }

  public Long getTackles() {
    return tackles;
  }

  public Long getInterceptions() {
    return interceptions;
  }

  public Long getClearances() {
    return clearances;
  }

  public BigDecimal getExpectedGoals() {
    return expectedGoals;
  }

  public BigDecimal getExpectedAssists() {
    return expectedAssists;
  }

  public BigDecimal getAverageRating() {
    return averageRating;
  }

  public record PlayerSeasonStatisticSnapshot(
      Long appearances,
      Long minutesPlayed,
      Long goals,
      Long assists,
      Long totalShots,
      Long shotsOnTarget,
      Long accuratePasses,
      Long totalPasses,
      Long keyPasses,
      Long tackles,
      Long interceptions,
      Long clearances,
      BigDecimal expectedGoals,
      BigDecimal expectedAssists,
      BigDecimal averageRating) {}
}
