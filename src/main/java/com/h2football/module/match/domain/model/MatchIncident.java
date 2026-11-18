package com.h2football.module.match.domain.model;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.player.domain.model.Player;
import jakarta.persistence.*;

@Entity
@Table(
    name = "match_incidents",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_match_incident_source",
            columnNames = {"match_id", "provider", "external_id"}))
public class MatchIncident {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Column(name = "external_id", nullable = false, length = 120)
  private String externalId;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "incident_type", nullable = false, length = 40)
  private String incidentType;

  @Column(name = "incident_class", length = 40)
  private String incidentClass;

  @Column(name = "minute")
  private Integer minute;

  @Column(name = "added_time")
  private Integer addedTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IncidentSide side;

  @Column(length = 160)
  private String text;

  @Column(length = 160)
  private String reason;

  @Column(name = "home_score")
  private Integer homeScore;

  @Column(name = "away_score")
  private Integer awayScore;

  @Column(nullable = false)
  private boolean rescinded;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_id")
  private Player player;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assist_player_id")
  private Player assistPlayer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_in_id")
  private Player playerIn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_out_id")
  private Player playerOut;

  protected MatchIncident() {}

  public MatchIncident(
      Match match,
      Provider provider,
      String externalId,
      int sortOrder,
      String incidentType,
      String incidentClass,
      Integer minute,
      Integer addedTime,
      IncidentSide side,
      String text,
      String reason,
      Integer homeScore,
      Integer awayScore,
      boolean rescinded,
      Player player,
      Player assistPlayer,
      Player playerIn,
      Player playerOut) {
    if (match == null || provider == null || side == null)
      throw new IllegalArgumentException("Match, provider and side are required");
    this.match = match;
    this.provider = provider;
    this.externalId = required(externalId, 120);
    this.sortOrder = sortOrder;
    this.incidentType = required(incidentType, 40);
    this.incidentClass = normalize(incidentClass, 40);
    this.minute = minute;
    this.addedTime = addedTime;
    this.side = side;
    this.text = normalize(text, 160);
    this.reason = normalize(reason, 160);
    this.homeScore = homeScore;
    this.awayScore = awayScore;
    this.rescinded = rescinded;
    this.player = player;
    this.assistPlayer = assistPlayer;
    this.playerIn = playerIn;
    this.playerOut = playerOut;
  }

  private String required(String value, int max) {
    String result = normalize(value, max);
    if (result == null) throw new IllegalArgumentException("Incident field is required");
    return result;
  }

  private String normalize(String value, int max) {
    if (value == null || value.isBlank()) return null;
    String result = value.trim();
    if (result.length() > max)
      throw new IllegalArgumentException("Incident field exceeds " + max + " characters");
    return result;
  }

  public Long getId() {
    return id;
  }

  public String getExternalId() {
    return externalId;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getIncidentType() {
    return incidentType;
  }

  public String getIncidentClass() {
    return incidentClass;
  }

  public Integer getMinute() {
    return minute;
  }

  public Integer getAddedTime() {
    return addedTime;
  }

  public IncidentSide getSide() {
    return side;
  }

  public String getText() {
    return text;
  }

  public String getReason() {
    return reason;
  }

  public Integer getHomeScore() {
    return homeScore;
  }

  public Integer getAwayScore() {
    return awayScore;
  }

  public boolean isRescinded() {
    return rescinded;
  }

  public Player getPlayer() {
    return player;
  }

  public Player getAssistPlayer() {
    return assistPlayer;
  }

  public Player getPlayerIn() {
    return playerIn;
  }

  public Player getPlayerOut() {
    return playerOut;
  }
}
