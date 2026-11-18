package com.h2football.module.masterdata.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "countries")
public class Country {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "iso_code", nullable = false, length = 2, unique = true)
  private String isoCode;

  @Column(nullable = false, length = 120, unique = true)
  private String name;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private Instant updatedAt;

  protected Country() {}

  public Country(String isoCode, String name) {
    this.isoCode = isoCode;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getIsoCode() {
    return isoCode;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
