package com.h2football.module.match.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchLineup;
import com.h2football.module.match.domain.model.MatchLineupPlayer;
import com.h2football.module.match.domain.repository.MatchLineupRepository;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchLineupApplicationService {
  private final MatchRepository matches;
  private final TeamRepository teams;
  private final PlayerRepository players;
  private final MatchLineupRepository lineups;

  public MatchLineupApplicationService(
      MatchRepository matches,
      TeamRepository teams,
      PlayerRepository players,
      MatchLineupRepository lineups) {
    this.matches = matches;
    this.teams = teams;
    this.players = players;
    this.lineups = lineups;
  }

  public int replaceFromProvider(Long matchId, Provider provider, List<TeamLineupInput> inputs) {
    validateMatchAndProvider(matchId, provider);
    if (inputs == null || inputs.isEmpty()) return 0;
    Match match = matches.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    List<MatchLineup> replacement = new ArrayList<>();
    for (TeamLineupInput input : inputs) {
      var team =
          teams
              .findById(input.teamId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Unknown canonical team " + input.teamId()));
      if (!team.getId().equals(match.getHomeTeam().getId())
          && !team.getId().equals(match.getAwayTeam().getId())) {
        throw new IllegalArgumentException("Lineup team is not a participant in the match");
      }
      MatchLineup lineup =
          new MatchLineup(match, team, provider, input.formation(), input.confirmed());
      for (LineupPlayerInput playerInput : input.players()) {
        var player =
            players
                .findById(playerInput.playerId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Unknown canonical player " + playerInput.playerId()));
        lineup.addPlayer(
            new MatchLineupPlayer(
                lineup,
                player,
                playerInput.role(),
                playerInput.position(),
                playerInput.shirtNumber(),
                playerInput.captain(),
                playerInput.missingReason()));
      }
      replacement.add(lineup);
    }
    for (TeamLineupInput input : inputs) {
      lineups.deleteAllByMatchIdAndTeamIdAndProvider(matchId, input.teamId(), provider);
    }
    lineups.saveAll(replacement);
    return replacement.stream().mapToInt(item -> item.getPlayers().size()).sum();
  }

  @Transactional(readOnly = true)
  public List<MatchLineup> findByMatch(Long matchId, Provider provider) {
    validateMatchAndProvider(matchId, provider);
    if (!matches.existsById(matchId)) throw new MatchNotFoundException(matchId);
    return lineups.findAllByMatchIdAndProviderOrderByTeamIdAsc(matchId, provider);
  }

  private void validateMatchAndProvider(Long matchId, Provider provider) {
    if (matchId == null || matchId <= 0)
      throw new IllegalArgumentException("matchId must be positive");
    if (provider == null) throw new IllegalArgumentException("provider must not be null");
  }

  public record TeamLineupInput(
      Long teamId, String formation, boolean confirmed, List<LineupPlayerInput> players) {
    public TeamLineupInput {
      players = players == null ? List.of() : List.copyOf(players);
    }
  }

  public record LineupPlayerInput(
      Long playerId,
      com.h2football.module.match.domain.model.LineupRole role,
      String position,
      String shirtNumber,
      boolean captain,
      String missingReason) {}
}
