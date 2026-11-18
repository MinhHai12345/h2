package com.h2football.module.player.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Player {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "short_name", length = 80)
  private String shortName;

  @Column(length = 120)
  private String slug;

  @Column(length = 10)
  private String position;

  @Column(name = "country_code", length = 3)
  private String countryCode;

  @Column(name = "country_name", length = 100)
  private String countryName;

  protected Player() {}

  public Player(
      String name,
      String shortName,
      String slug,
      String position,
      String countryCode,
      String countryName) {
    updateProfile(name, shortName, slug, position, countryCode, countryName);
  }

  public void updateProfile(
      String name,
      String shortName,
      String slug,
      String position,
      String countryCode,
      String countryName) {
    String normalizedName = normalize(name, 120);
    if (normalizedName == null) throw new IllegalArgumentException("Player name is required");
    this.name = normalizedName;
    this.shortName = merge(this.shortName, shortName, 80);
    this.slug = merge(this.slug, slug, 120);
    this.position = merge(this.position, position, 10);
    this.countryCode = merge(this.countryCode, countryCode, 3);
    this.countryName = merge(this.countryName, countryName, 100);
  }

  private String merge(String existing, String incoming, int max) {
    String normalized = normalize(incoming, max);
    return normalized == null ? existing : normalized;
  }

  private String normalize(String value, int max) {
    if (value == null || value.isBlank()) return null;
    String result = value.trim();
    if (result.length() > max)
      throw new IllegalArgumentException("Player field exceeds " + max + " characters");
    return result;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getShortName() {
    return shortName;
  }

  public String getSlug() {
    return slug;
  }

  public String getPosition() {
    return position;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public String getCountryName() {
    return countryName;
  }
}
