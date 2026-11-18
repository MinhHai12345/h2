package com.h2football.module.provider.sofascore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import java.util.List;
import org.junit.jupiter.api.Test;

class SofaScoreMatchEventMapperTest {
  private final SofaScoreMatchEventMapper mapper = new SofaScoreMatchEventMapper();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void mapsStartingPlayersSubstitutesAndMissingPlayers() {
    var player =
        new SofaScoreDtos.PlayerProfile(
            101L,
            "Player One",
            "P. One",
            "player-one",
            "F",
            "9",
            new SofaScoreDtos.Country("BR", "Brazil"));
    var response =
        new SofaScoreDtos.EventLineupsResponse(
            true,
            new SofaScoreDtos.TeamLineup(
                "4-3-3",
                List.of(new SofaScoreDtos.LineupPlayer(player, "F", "9", null, false, true, null)),
                List.of(
                    new SofaScoreDtos.MissingPlayer(
                        new SofaScoreDtos.PlayerProfile(
                            102L, "Player Two", null, null, "M", null, null),
                        "Injury"))),
            new SofaScoreDtos.TeamLineup("4-4-2", List.of(), List.of()));

    var snapshot = mapper.toLineup(response);

    assertThat(snapshot.confirmed()).isTrue();
    assertThat(snapshot.home().formation()).isEqualTo("4-3-3");
    assertThat(snapshot.home().players()).hasSize(2);
    assertThat(snapshot.home().players().get(0).captain()).isTrue();
    assertThat(snapshot.home().players().get(1).missingReason()).isEqualTo("Injury");
    assertThat(snapshot.away().formation()).isEqualTo("4-4-2");
  }

  @Test
  void mapsNestedPlayerStatisticsAndRetainsUnknownKeys() throws Exception {
    var player =
        new SofaScoreDtos.PlayerProfile(
            101L, "Player One", null, null, "F", "9", new SofaScoreDtos.Country("BR", "Brazil"));
    var stats =
        objectMapper.readTree(
            """
            {
              "rating": 7.8,
              "minutesPlayed": 90,
              "goals": 1,
              "expectedGoals": 0.72,
              "customMetric": "value"
            }
            """);
    var response =
        new SofaScoreDtos.EventLineupsResponse(
            true,
            new SofaScoreDtos.TeamLineup(
                "4-3-3",
                List.of(
                    new SofaScoreDtos.LineupPlayer(player, "F", "9", null, false, false, stats)),
                List.of()),
            new SofaScoreDtos.TeamLineup("4-4-2", List.of(), List.of()));

    var mapped = mapper.toLineup(response).home().players().get(0).statistics();

    assertThat(mapped.value("rating")).isEqualTo("7.8");
    assertThat(mapped.value("minutesPlayed")).isEqualTo("90");
    assertThat(mapped.value("expectedGoals")).isEqualTo("0.72");
    assertThat(mapped.value("customMetric")).isEqualTo("value");
    assertThat(mapped.rawJson()).contains("\"customMetric\":\"value\"");
  }

  @Test
  void mapsIncidentSideAndNormalizesPeriodAddedTime() {
    var response =
        new SofaScoreDtos.EventIncidentsResponse(
            List.of(
                new SofaScoreDtos.Incident(
                    9001L, 90, 999, true, "FT", null, 2, 1, "period", null, false, null, null, null,
                    null),
                new SofaScoreDtos.Incident(
                    9002L, 45, 2, false, null, "Foul", null, null, "card", "yellow", false, null,
                    null, null, null)));

    var incidents = mapper.toIncidents(777L, response);

    assertThat(incidents).hasSize(2);
    assertThat(incidents.get(0).externalId()).isEqualTo("9001");
    assertThat(incidents.get(0).isHome()).isTrue();
    assertThat(incidents.get(0).addedTime()).isNull();
    assertThat(incidents.get(1).isHome()).isFalse();
    assertThat(incidents.get(1).incidentClass()).isEqualTo("yellow");
  }
}
