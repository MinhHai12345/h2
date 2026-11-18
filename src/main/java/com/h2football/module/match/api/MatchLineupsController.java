package com.h2football.module.match.api;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.application.MatchLineupApplicationService;
import com.h2football.module.match.domain.model.MatchLineup;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/lineups")
public class MatchLineupsController {
  private final MatchLineupApplicationService lineups;

  public MatchLineupsController(MatchLineupApplicationService lineups) {
    this.lineups = lineups;
  }

  @GetMapping
  public List<LineupResponse> find(
      @PathVariable Long matchId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return lineups.findByMatch(matchId, provider).stream().map(LineupResponse::from).toList();
  }

  public record LineupResponse(
      Long teamId,
      String teamName,
      String formation,
      boolean confirmed,
      List<LineupPlayerResponse> players) {
    static LineupResponse from(MatchLineup item) {
      return new LineupResponse(
          item.getTeam().getId(),
          item.getTeam().getName(),
          item.getFormation(),
          item.isConfirmed(),
          item.getPlayers().stream()
              .map(
                  player ->
                      new LineupPlayerResponse(
                          player.getPlayer().getId(),
                          player.getPlayer().getName(),
                          player.getRole(),
                          player.getPosition(),
                          player.getShirtNumber(),
                          player.isCaptain(),
                          player.getMissingReason()))
              .toList());
    }
  }

  public record LineupPlayerResponse(
      Long playerId,
      String playerName,
      com.h2football.module.match.domain.model.LineupRole role,
      String position,
      String shirtNumber,
      boolean captain,
      String missingReason) {}
}
