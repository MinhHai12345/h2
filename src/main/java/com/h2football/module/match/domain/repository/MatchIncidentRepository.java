package com.h2football.module.match.domain.repository;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchIncident;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchIncidentRepository extends JpaRepository<MatchIncident, Long> {
  void deleteAllByMatchIdAndProvider(Long matchId, Provider provider);

  @EntityGraph(attributePaths = {"player", "assistPlayer", "playerIn", "playerOut"})
  List<MatchIncident> findAllByMatchIdAndProviderOrderBySortOrderAsc(
      Long matchId, Provider provider);
}
