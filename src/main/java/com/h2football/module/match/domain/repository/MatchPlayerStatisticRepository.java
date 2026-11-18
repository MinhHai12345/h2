package com.h2football.module.match.domain.repository;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPlayerStatisticRepository extends JpaRepository<MatchPlayerStatistic, Long> {
  Optional<MatchPlayerStatistic> findByMatchIdAndPlayerIdAndProvider(
      Long matchId, Long playerId, Provider provider);

  @EntityGraph(attributePaths = {"player"})
  List<MatchPlayerStatistic> findAllByMatchIdAndProviderOrderByPlayerNameAsc(
      Long matchId, Provider provider);
}
