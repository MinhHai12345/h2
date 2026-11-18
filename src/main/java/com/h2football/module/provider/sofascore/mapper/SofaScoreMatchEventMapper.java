package com.h2football.module.provider.sofascore.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import com.h2football.module.provider.sofascore.model.SofaScoreIncidentSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreLineupSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScorePlayerSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScorePlayerStatisticsSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreMatchEventMapper {
  public SofaScoreLineupSnapshot toLineup(SofaScoreDtos.EventLineupsResponse response) {
    if (response == null) return null;
    return new SofaScoreLineupSnapshot(
        Boolean.TRUE.equals(response.confirmed()),
        mapTeamLineup(response.home()),
        mapTeamLineup(response.away()));
  }

  private SofaScoreLineupSnapshot.TeamLineup mapTeamLineup(SofaScoreDtos.TeamLineup source) {
    if (source == null) return new SofaScoreLineupSnapshot.TeamLineup(null, List.of());
    List<SofaScoreLineupSnapshot.LineupPlayer> players = new ArrayList<>();
    if (source.players() != null) {
      for (SofaScoreDtos.LineupPlayer item : source.players()) {
        SofaScorePlayerSnapshot player = mapPlayer(item == null ? null : item.player());
        if (player == null) continue;
        players.add(
            new SofaScoreLineupSnapshot.LineupPlayer(
                player,
                first(item.position(), item.player().position()),
                first(item.shirtNumber(), item.jerseyNumber(), item.player().jerseyNumber()),
                Boolean.TRUE.equals(item.substitute()),
                Boolean.TRUE.equals(item.captain()),
                null,
                mapPlayerStatistics(item.statistics())));
      }
    }
    if (source.missingPlayers() != null) {
      for (SofaScoreDtos.MissingPlayer item : source.missingPlayers()) {
        SofaScorePlayerSnapshot player = mapPlayer(item == null ? null : item.player());
        if (player != null) {
          players.add(
              new SofaScoreLineupSnapshot.LineupPlayer(
                  player, player.position(), null, false, false, item.reason(), null));
        }
      }
    }
    return new SofaScoreLineupSnapshot.TeamLineup(source.formation(), players);
  }

  private SofaScorePlayerStatisticsSnapshot mapPlayerStatistics(JsonNode source) {
    if (source == null || !source.isObject() || source.isEmpty()) return null;
    Map<String, String> values = new LinkedHashMap<>();
    source
        .fields()
        .forEachRemaining(
            entry -> {
              JsonNode value = entry.getValue();
              if (value != null && !value.isNull()) {
                JsonNode normalized =
                    value.isObject() && value.has("value") ? value.get("value") : value;
                if (normalized.isValueNode()) {
                  values.put(entry.getKey(), normalized.asText());
                }
              }
            });
    return new SofaScorePlayerStatisticsSnapshot(source.toString(), values);
  }

  public List<SofaScoreIncidentSnapshot> toIncidents(
      long eventId, SofaScoreDtos.EventIncidentsResponse response) {
    if (response == null || response.incidents() == null) return List.of();
    List<SofaScoreIncidentSnapshot> result = new ArrayList<>();
    int order = 0;
    for (SofaScoreDtos.Incident item : response.incidents()) {
      if (item == null || item.incidentType() == null || item.incidentType().isBlank()) {
        order++;
        continue;
      }
      String externalId = item.id() == null ? eventId + ":incident:" + order : item.id().toString();
      result.add(
          new SofaScoreIncidentSnapshot(
              externalId,
              order,
              item.incidentType(),
              item.incidentClass(),
              item.time(),
              normalizeAddedTime(item.addedTime()),
              item.isHome(),
              item.text(),
              item.reason(),
              item.homeScore(),
              item.awayScore(),
              Boolean.TRUE.equals(item.rescinded()),
              mapPlayer(item.player()),
              mapPlayer(item.assist1()),
              mapPlayer(item.playerIn()),
              mapPlayer(item.playerOut())));
      order++;
    }
    return List.copyOf(result);
  }

  private Integer normalizeAddedTime(Integer addedTime) {
    return addedTime == null || addedTime == 999 ? null : addedTime;
  }

  private SofaScorePlayerSnapshot mapPlayer(SofaScoreDtos.PlayerProfile source) {
    if (source == null
        || source.id() == null
        || source.id() <= 0
        || source.name() == null
        || source.name().isBlank()) return null;
    return new SofaScorePlayerSnapshot(
        source.id().toString(),
        source.name(),
        source.shortName(),
        source.slug(),
        source.position(),
        source.country() == null ? null : source.country().alpha2(),
        source.country() == null ? null : source.country().name());
  }

  private String first(String... values) {
    for (String value : values) if (value != null && !value.isBlank()) return value.trim();
    return null;
  }
}
