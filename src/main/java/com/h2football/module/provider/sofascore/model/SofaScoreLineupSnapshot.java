package com.h2football.module.provider.sofascore.model;

import java.util.List;

public record SofaScoreLineupSnapshot(boolean confirmed, TeamLineup home, TeamLineup away) {
  public record TeamLineup(String formation, List<LineupPlayer> players) {
    public TeamLineup {
      players = players == null ? List.of() : List.copyOf(players);
    }
  }

  public record LineupPlayer(
      SofaScorePlayerSnapshot player,
      String position,
      String shirtNumber,
      boolean substitute,
      boolean captain,
      String missingReason) {}
}
