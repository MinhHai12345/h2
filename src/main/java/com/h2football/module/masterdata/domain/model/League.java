package com.h2football.module.masterdata.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "leagues")
public class League {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80, unique = true)
    private String code;
    @Column(nullable = false, length = 160)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected League() {}
    public League(String code, String name, Country country) {
        this.code = code; this.name = name; this.country = country;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Country getCountry() { return country; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}