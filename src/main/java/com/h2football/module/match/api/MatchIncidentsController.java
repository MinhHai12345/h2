package com.h2football.module.match.api;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.application.MatchIncidentApplicationService;
import com.h2football.module.match.domain.model.IncidentSide;
import com.h2football.module.match.domain.model.MatchIncident;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/incidents")
public class MatchIncidentsController {
  private final MatchIncidentApplicationService incidents;

  public MatchIncidentsController(MatchIncidentApplicationService incidents) {
    this.incidents = incidents;
  }

  @GetMapping
  public List<IncidentResponse> find(
      @PathVariable Long matchId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return incidents.findByMatch(matchId, provider).stream().map(IncidentResponse::from).toList();
  }

  public record IncidentResponse(
      Long id,
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
      Long playerId,
      String playerName,
      Long assistPlayerId,
      String assistPlayerName,
      Long playerInId,
      String playerInName,
      Long playerOutId,
      String playerOutName) {
    static IncidentResponse from(MatchIncident item) {
      return new IncidentResponse(
          item.getId(),
          item.getExternalId(),
          item.getSortOrder(),
          item.getIncidentType(),
          item.getIncidentClass(),
          item.getMinute(),
          item.getAddedTime(),
          item.getSide(),
          item.getText(),
          item.getReason(),
          item.getHomeScore(),
          item.getAwayScore(),
          item.isRescinded(),
          id(item.getPlayer()),
          name(item.getPlayer()),
          id(item.getAssistPlayer()),
          name(item.getAssistPlayer()),
          id(item.getPlayerIn()),
          name(item.getPlayerIn()),
          id(item.getPlayerOut()),
          name(item.getPlayerOut()));
    }

    private static Long id(com.h2football.module.player.domain.model.Player player) {
      return player == null ? null : player.getId();
    }

    private static String name(com.h2football.module.player.domain.model.Player player) {
      return player == null ? null : player.getName();
    }
  }
}
