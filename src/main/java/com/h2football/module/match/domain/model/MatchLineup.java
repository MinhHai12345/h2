package com.h2football.module.match.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Team;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "match_lineups",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_match_lineup_source",
            columnNames = {"match_id", "team_id", "provider"}))
public class MatchLineup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Column(length = 20)
  private String formation;

  @Column(nullable = false)
  private boolean confirmed;

  @OneToMany(mappedBy = "lineup", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MatchLineupPlayer> players = new ArrayList<>();

  protected MatchLineup() {}

  public MatchLineup(
      Match match, Team team, Provider provider, String formation, boolean confirmed) {
    if (match == null || team == null || provider == null)
      throw new IllegalArgumentException("Match, team and provider are required");
    this.match = match;
    this.team = team;
    this.provider = provider;
    this.formation = normalize(formation, 20);
    this.confirmed = confirmed;
  }

  public void addPlayer(MatchLineupPlayer player) {
    if (player == null) throw new IllegalArgumentException("Lineup player is required");
    players.add(player);
  }

  private String normalize(String value, int max) {
    if (value == null || value.isBlank()) return null;
    String result = value.trim();
    if (result.length() > max)
      throw new IllegalArgumentException("Lineup field exceeds " + max + " characters");
    return result;
  }

  public Long getId() {
    return id;
  }

  public Match getMatch() {
    return match;
  }

  public Team getTeam() {
    return team;
  }

  public Provider getProvider() {
    return provider;
  }

  public String getFormation() {
    return formation;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public List<MatchLineupPlayer> getPlayers() {
    return players;
  }
}
