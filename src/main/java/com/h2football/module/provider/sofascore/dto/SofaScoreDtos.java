package com.h2football.module.provider.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class SofaScoreDtos {
    private SofaScoreDtos() { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TournamentCatalogue(@JsonProperty("uniqueTournaments") List<Tournament> uniqueTournaments) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tournament(Long id, String name, String slug, Category category) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Category(Long id, String name, String alpha2) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeasonsResponse(@JsonProperty("seasons") List<Season> seasons) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Season(Long id, String name, String year, @JsonProperty("isCurrent") boolean current) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingsResponse(@JsonProperty("standings") List<StandingGroup> standings) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingGroup(String type, String name, List<StandingRow> rows) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingRow(Team team) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Team(Long id, String name, String slug, String shortName, Country country) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Country(String alpha2, String name) { }
}
