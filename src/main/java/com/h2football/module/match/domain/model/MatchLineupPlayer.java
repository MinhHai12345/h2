package com.h2football.module.match.domain.model;

import com.h2football.module.player.domain.model.Player;
import jakarta.persistence.*;

@Entity
@Table(
    name = "match_lineup_players",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_match_lineup_player_role",
            columnNames = {"lineup_id", "player_id", "player_role"}))
public class MatchLineupPlayer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lineup_id", nullable = false)
  private MatchLineup lineup;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Enumerated(EnumType.STRING)
  @Column(name = "player_role", nullable = false, length = 20)
  private LineupRole role;

  @Column(length = 10)
  private String position;

  @Column(name = "shirt_number", length = 10)
  private String shirtNumber;

  @Column(nullable = false)
  private boolean captain;

  @Column(name = "missing_reason", length = 120)
  private String missingReason;

  protected MatchLineupPlayer() {}

  public MatchLineupPlayer(
      MatchLineup lineup,
      Player player,
      LineupRole role,
      String position,
      String shirtNumber,
      boolean captain,
      String missingReason) {
    if (lineup == null || player == null || role == null)
      throw new IllegalArgumentException("Lineup, player and role are required");
    this.lineup = lineup;
    this.player = player;
    this.role = role;
    this.position = normalize(position, 10);
    this.shirtNumber = normalize(shirtNumber, 10);
    this.captain = captain;
    this.missingReason = normalize(missingReason, 120);
  }

  private String normalize(String value, int max) {
    if (value == null || value.isBlank()) return null;
    String result = value.trim();
    if (result.length() > max)
      throw new IllegalArgumentException("Lineup player field exceeds " + max + " characters");
    return result;
  }

  public Long getId() {
    return id;
  }

  public Player getPlayer() {
    return player;
  }

  public LineupRole getRole() {
    return role;
  }

  public String getPosition() {
    return position;
  }

  public String getShirtNumber() {
    return shirtNumber;
  }

  public boolean isCaptain() {
    return captain;
  }

  public String getMissingReason() {
    return missingReason;
  }
}
