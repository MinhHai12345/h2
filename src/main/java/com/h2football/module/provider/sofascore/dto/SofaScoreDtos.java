package com.h2football.module.provider.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public final class SofaScoreDtos {
  private SofaScoreDtos() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TournamentCatalogue(
      @JsonProperty("uniqueTournaments") List<Tournament> uniqueTournaments) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Tournament(Long id, String name, String slug, Category category) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Category(Long id, String name, String alpha2) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SeasonsResponse(@JsonProperty("seasons") List<Season> seasons) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Season(
      Long id, String name, String year, @JsonProperty("isCurrent") boolean current) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StandingsResponse(@JsonProperty("standings") List<StandingGroup> standings) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StandingGroup(String type, String name, List<StandingRow> rows) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StandingRow(Team team) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Team(Long id, String name, String slug, String shortName, Country country) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Country(String alpha2, String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record EventsResponse(List<Event> events) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Event(
      Long id,
      Long startTimestamp,
      Team homeTeam,
      Team awayTeam,
      EventStatus status,
      Score homeScore,
      Score awayScore,
      RoundInfo roundInfo) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record EventStatus(String code, String description, String type) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Score(Integer current, Integer display) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RoundInfo(String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record EventStatisticsResponse(
      @JsonProperty("statistics") List<StatisticPeriod> statistics) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StatisticPeriod(String period, List<StatisticGroup> groups) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StatisticGroup(
      @JsonProperty("groupName") String groupName,
      @JsonProperty("statisticsItems") List<StatisticItem> statisticsItems) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StatisticItem(
      String name,
      String key,
      String home,
      String away,
      BigDecimal homeValue,
      BigDecimal awayValue) {}
}
