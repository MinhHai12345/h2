package com.h2football.module.match.domain.repository;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchPlayerStatisticRepository extends JpaRepository<MatchPlayerStatistic, Long> {
  Optional<MatchPlayerStatistic> findByMatchIdAndPlayerIdAndProvider(
      Long matchId, Long playerId, Provider provider);

  @EntityGraph(attributePaths = {"player"})
  List<MatchPlayerStatistic> findAllByMatchIdAndProviderOrderByPlayerNameAsc(
      Long matchId, Provider provider);

  @Query(
      """
      select
        statistic.player.id as playerId,
        statistic.player.name as playerName,
        count(statistic.id) as appearances,
        coalesce(sum(statistic.minutesPlayed), 0) as minutesPlayed,
        coalesce(sum(statistic.goals), 0) as goals,
        coalesce(sum(statistic.assists), 0) as assists,
        coalesce(sum(statistic.totalShots), 0) as totalShots,
        coalesce(sum(statistic.shotsOnTarget), 0) as shotsOnTarget,
        coalesce(sum(statistic.accuratePasses), 0) as accuratePasses,
        coalesce(sum(statistic.totalPasses), 0) as totalPasses,
        coalesce(sum(statistic.keyPasses), 0) as keyPasses,
        coalesce(sum(statistic.tackles), 0) as tackles,
        coalesce(sum(statistic.interceptions), 0) as interceptions,
        coalesce(sum(statistic.clearances), 0) as clearances,
        sum(statistic.expectedGoals) as expectedGoals,
        sum(statistic.expectedAssists) as expectedAssists,
        avg(statistic.rating) as averageRating
      from MatchPlayerStatistic statistic
      where statistic.match.season.id = :seasonId
        and statistic.provider = :provider
      group by statistic.player.id, statistic.player.name
      order by sum(statistic.goals) desc, avg(statistic.rating) desc
      """)
  List<SeasonPlayerStatisticProjection> aggregateBySeasonAndProvider(
      @Param("seasonId") Long seasonId, @Param("provider") Provider provider);
}
