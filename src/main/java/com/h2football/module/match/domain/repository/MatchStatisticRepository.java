package com.h2football.module.match.domain.repository;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchStatistic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchStatisticRepository extends JpaRepository<MatchStatistic, Long> {
  List<MatchStatistic> findAllByMatchIdOrderByPeriodAscGroupNameAscStatisticKeyAsc(Long matchId);

  void deleteAllByMatchIdAndProvider(Long matchId, Provider provider);

  @Query(
      value =
          """
          SELECT
            team_rows.team_id AS teamId,
            team_rows.period AS period,
            team_rows.group_name AS groupName,
            team_rows.statistic_key AS statisticKey,
            MAX(team_rows.statistic_name) AS statisticName,
            COUNT(DISTINCT team_rows.match_id) AS matchesCount,
            SUM(team_rows.statistic_value) AS totalValue,
            AVG(team_rows.statistic_value) AS averageValue
          FROM (
            SELECT
              match_record.home_team_id AS team_id,
              statistic.match_id AS match_id,
              statistic.period AS period,
              statistic.group_name AS group_name,
              statistic.statistic_key AS statistic_key,
              statistic.statistic_name AS statistic_name,
              statistic.home_value AS statistic_value
            FROM match_statistics statistic
            JOIN matches match_record ON match_record.id = statistic.match_id
            WHERE match_record.season_id = :seasonId
              AND match_record.status = 'FINISHED'
              AND statistic.provider = :provider
              AND statistic.home_value IS NOT NULL
            UNION ALL
            SELECT
              match_record.away_team_id AS team_id,
              statistic.match_id AS match_id,
              statistic.period AS period,
              statistic.group_name AS group_name,
              statistic.statistic_key AS statistic_key,
              statistic.statistic_name AS statistic_name,
              statistic.away_value AS statistic_value
            FROM match_statistics statistic
            JOIN matches match_record ON match_record.id = statistic.match_id
            WHERE match_record.season_id = :seasonId
              AND match_record.status = 'FINISHED'
              AND statistic.provider = :provider
              AND statistic.away_value IS NOT NULL
          ) team_rows
          GROUP BY team_rows.team_id, team_rows.period, team_rows.group_name,
                   team_rows.statistic_key
          ORDER BY team_rows.team_id, team_rows.period, team_rows.group_name,
                   team_rows.statistic_key
          """,
      nativeQuery = true)
  List<TeamSeasonStatisticProjection> aggregateTeamStatisticsBySeason(
      @Param("seasonId") Long seasonId, @Param("provider") String provider);
}
