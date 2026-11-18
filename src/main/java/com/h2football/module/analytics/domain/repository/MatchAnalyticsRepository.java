package com.h2football.module.analytics.domain.repository;

import com.h2football.module.analytics.domain.model.MatchAnalytics;
import com.h2football.module.identity.domain.model.Provider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAnalyticsRepository extends JpaRepository<MatchAnalytics, Long> {
  @EntityGraph(attributePaths = {"match", "match.season", "match.homeTeam", "match.awayTeam"})
  Optional<MatchAnalytics> findByMatchIdAndProvider(Long matchId, Provider provider);

  @EntityGraph(attributePaths = {"match", "match.homeTeam", "match.awayTeam"})
  List<MatchAnalytics> findAllByMatchSeasonIdAndProviderOrderByMatchKickoffAtAsc(
      Long seasonId, Provider provider);
}
