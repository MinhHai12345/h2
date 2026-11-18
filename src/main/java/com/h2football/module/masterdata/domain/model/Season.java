package com.h2football.module.masterdata.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "seasons",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_seasons_league_code",
            columnNames = {"league_id", "season_code"}))
public class Season {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "league_id", nullable = false)
  private League league;

  @Column(name = "season_code", nullable = false, length = 30)
  private String seasonCode;

  @Column(name = "display_name", nullable = false, length = 80)
  private String displayName;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private Instant updatedAt;

  protected Season() {}

  public Season(
      League league,
      String seasonCode,
      String displayName,
      LocalDate startDate,
      LocalDate endDate) {
    this.league = league;
    this.seasonCode = seasonCode;
    this.displayName = displayName;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public Long getId() {
    return id;
  }

  public League getLeague() {
    return league;
  }

  public String getSeasonCode() {
    return seasonCode;
  }

  public String getDisplayName() {
    return displayName;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
