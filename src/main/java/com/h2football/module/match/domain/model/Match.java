package com.h2football.module.match.domain.model;

import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "matches")
public class Match {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "home_team_id", nullable = false)
  private Team homeTeam;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "away_team_id", nullable = false)
  private Team awayTeam;

  @Column(name = "kickoff_at", nullable = false)
  private Instant kickoffAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MatchStatus status;

  @Column(name = "home_score")
  private Integer homeScore;

  @Column(name = "away_score")
  private Integer awayScore;

  @Column(name = "round_name", length = 80)
  private String roundName;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private Instant updatedAt;

  protected Match() {}

  public Match(
      Season season,
      Team homeTeam,
      Team awayTeam,
      Instant kickoffAt,
      MatchStatus status,
      Integer homeScore,
      Integer awayScore,
      String roundName) {
    if (season == null
        || homeTeam == null
        || awayTeam == null
        || kickoffAt == null
        || status == null) {
      throw new IllegalArgumentException("Season, teams, kickoffAt and status are required");
    }
    if (homeTeam.getId() != null && homeTeam.getId().equals(awayTeam.getId())) {
      throw new IllegalArgumentException("Home team and away team must be different");
    }
    this.season = season;
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.kickoffAt = kickoffAt;
    this.status = status;
    this.homeScore = homeScore;
    this.awayScore = awayScore;
    this.roundName = normalizeRoundName(roundName);
  }

  public void updateSnapshot(
      Instant kickoffAt,
      MatchStatus status,
      Integer homeScore,
      Integer awayScore,
      String roundName) {
    if (kickoffAt == null || status == null)
      throw new IllegalArgumentException("kickoffAt and status are required");
    this.kickoffAt = kickoffAt;
    this.status = status;
    if (homeScore != null) this.homeScore = homeScore;
    if (awayScore != null) this.awayScore = awayScore;
    this.roundName = normalizeRoundName(roundName);
  }

  private static String normalizeRoundName(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public Long getId() {
    return id;
  }

  public Season getSeason() {
    return season;
  }

  public Team getHomeTeam() {
    return homeTeam;
  }

  public Team getAwayTeam() {
    return awayTeam;
  }

  public Instant getKickoffAt() {
    return kickoffAt;
  }

  public MatchStatus getStatus() {
    return status;
  }

  public Integer getHomeScore() {
    return homeScore;
  }

  public Integer getAwayScore() {
    return awayScore;
  }

  public String getRoundName() {
    return roundName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
