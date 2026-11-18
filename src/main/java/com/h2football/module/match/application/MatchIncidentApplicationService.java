package com.h2football.module.match.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.*;
import com.h2football.module.match.domain.repository.MatchIncidentRepository;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.player.domain.repository.PlayerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchIncidentApplicationService {
  private final MatchRepository matches;
  private final PlayerRepository players;
  private final MatchIncidentRepository incidents;

  public MatchIncidentApplicationService(
      MatchRepository matches, PlayerRepository players, MatchIncidentRepository incidents) {
    this.matches = matches;
    this.players = players;
    this.incidents = incidents;
  }

  public int replaceFromProvider(Long matchId, Provider provider, List<IncidentInput> inputs) {
    if (matchId == null || matchId <= 0)
      throw new IllegalArgumentException("matchId must be positive");
    if (provider == null) throw new IllegalArgumentException("provider must not be null");
    if (!matches.existsById(matchId)) throw new MatchNotFoundException(matchId);
    if (inputs == null || inputs.isEmpty()) return 0;
    var match = matches.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    List<MatchIncident> replacement =
        inputs.stream()
            .map(
                input ->
                    new MatchIncident(
                        match,
                        provider,
                        input.externalId(),
                        input.sortOrder(),
                        input.incidentType(),
                        input.incidentClass(),
                        input.minute(),
                        input.addedTime(),
                        input.side(),
                        input.text(),
                        input.reason(),
                        input.homeScore(),
                        input.awayScore(),
                        input.rescinded(),
                        findPlayer(input.playerId()),
                        findPlayer(input.assistPlayerId()),
                        findPlayer(input.playerInId()),
                        findPlayer(input.playerOutId())))
            .toList();
    incidents.deleteAllByMatchIdAndProvider(matchId, provider);
    incidents.saveAll(replacement);
    return replacement.size();
  }

  @Transactional(readOnly = true)
  public List<MatchIncident> findByMatch(Long matchId, Provider provider) {
    if (matchId == null || matchId <= 0)
      throw new IllegalArgumentException("matchId must be positive");
    if (provider == null) throw new IllegalArgumentException("provider must not be null");
    if (!matches.existsById(matchId)) throw new MatchNotFoundException(matchId);
    return incidents.findAllByMatchIdAndProviderOrderBySortOrderAsc(matchId, provider);
  }

  private com.h2football.module.player.domain.model.Player findPlayer(Long id) {
    return id == null
        ? null
        : players
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown canonical player " + id));
  }

  public record IncidentInput(
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
      Long assistPlayerId,
      Long playerInId,
      Long playerOutId) {}
}
