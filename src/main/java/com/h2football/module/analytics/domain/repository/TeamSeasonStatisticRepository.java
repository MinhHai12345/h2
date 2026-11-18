package com.h2football.module.analytics.domain.repository;

import com.h2football.module.analytics.domain.model.TeamSeasonStatistic;
import com.h2football.module.identity.domain.model.Provider;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamSeasonStatisticRepository extends JpaRepository<TeamSeasonStatistic, Long> {
  void deleteAllBySeasonIdAndProvider(Long seasonId, Provider provider);

  @EntityGraph(attributePaths = {"team"})
  List<TeamSeasonStatistic>
      findAllBySeasonIdAndTeamIdAndProviderOrderByPeriodAscGroupNameAscStatisticKeyAsc(
          Long seasonId, Long teamId, Provider provider);

  @EntityGraph(attributePaths = {"team"})
  List<TeamSeasonStatistic> findAllBySeasonIdAndProvider(Long seasonId, Provider provider);
}
