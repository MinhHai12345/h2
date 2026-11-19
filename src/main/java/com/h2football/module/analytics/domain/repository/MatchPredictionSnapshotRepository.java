package com.h2football.module.analytics.domain.repository;

import com.h2football.module.analytics.domain.model.MatchPredictionSnapshot;
import com.h2football.module.identity.domain.model.Provider;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPredictionSnapshotRepository
    extends JpaRepository<MatchPredictionSnapshot, Long> {
  boolean existsByMatchIdAndProviderAndModelVersionAndComputedAt(
      Long matchId, Provider provider, String modelVersion, Instant computedAt);

  @EntityGraph(attributePaths = {"match", "match.season", "match.homeTeam", "match.awayTeam"})
  List<MatchPredictionSnapshot>
      findAllByMatchSeasonIdAndProviderOrderByMatchKickoffAtAscComputedAtAsc(
          Long seasonId, Provider provider);
}
