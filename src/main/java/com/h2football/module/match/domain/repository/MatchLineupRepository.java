package com.h2football.module.match.domain.repository;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchLineup;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {
  void deleteAllByMatchIdAndTeamIdAndProvider(Long matchId, Long teamId, Provider provider);

  @EntityGraph(attributePaths = {"team", "players", "players.player"})
  List<MatchLineup> findAllByMatchIdAndProviderOrderByTeamIdAsc(Long matchId, Provider provider);
}
