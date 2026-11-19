package com.h2football.module.workflow.application;

import com.h2football.module.identity.domain.model.Provider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataReadinessService {
  private static final String COUNTS_SQL =
      """
      SELECT
        COUNT(*) AS total_matches,
        SUM(CASE WHEN status = 'FINISHED' THEN 1 ELSE 0 END) AS finished_matches,
        SUM(CASE WHEN status = 'FINISHED'
          AND (home_score IS NULL OR away_score IS NULL) THEN 1 ELSE 0 END) AS finished_without_score
      FROM matches
      WHERE season_id = ?
      """;

  private static final String XG_COVERAGE_SQL =
      """
      SELECT COUNT(DISTINCT match_record.id)
      FROM matches match_record
      JOIN match_statistics statistic ON statistic.match_id = match_record.id
      WHERE match_record.season_id = ?
        AND match_record.status = 'FINISHED'
        AND statistic.provider = ?
        AND statistic.period = 'ALL'
        AND statistic.statistic_key = 'expectedGoals'
        AND statistic.home_value IS NOT NULL
        AND statistic.away_value IS NOT NULL
      """;

  private static final String LINEUP_COVERAGE_SQL =
      """
      SELECT COUNT(*)
      FROM (
        SELECT lineup.match_id
        FROM match_lineups lineup
        JOIN matches match_record ON match_record.id = lineup.match_id
        WHERE match_record.season_id = ?
          AND match_record.status = 'FINISHED'
          AND lineup.provider = ?
          AND lineup.confirmed = TRUE
        GROUP BY lineup.match_id
        HAVING COUNT(DISTINCT lineup.team_id) = 2
      ) complete_lineups
      """;

  private static final String INCIDENT_COVERAGE_SQL =
      """
      SELECT COUNT(DISTINCT incident.match_id)
      FROM match_incidents incident
      JOIN matches match_record ON match_record.id = incident.match_id
      WHERE match_record.season_id = ?
        AND match_record.status = 'FINISHED'
        AND incident.provider = ?
      """;

  private static final String PLAYER_STAT_COVERAGE_SQL =
      """
      SELECT COUNT(DISTINCT statistic.match_id)
      FROM match_player_statistics statistic
      JOIN matches match_record ON match_record.id = statistic.match_id
      WHERE match_record.season_id = ?
        AND match_record.status = 'FINISHED'
        AND statistic.provider = ?
      """;

  private static final String FRESHNESS_SQL =
      """
      SELECT MAX(updated_at) AS latest_update
      FROM (
        SELECT match_record.updated_at AS updated_at
        FROM matches match_record
        WHERE match_record.season_id = ?
        UNION ALL
        SELECT statistic.updated_at
        FROM match_statistics statistic
        JOIN matches match_record ON match_record.id = statistic.match_id
        WHERE match_record.season_id = ? AND statistic.provider = ?
        UNION ALL
        SELECT lineup.updated_at
        FROM match_lineups lineup
        JOIN matches match_record ON match_record.id = lineup.match_id
        WHERE match_record.season_id = ? AND lineup.provider = ?
        UNION ALL
        SELECT statistic.updated_at
        FROM match_player_statistics statistic
        JOIN matches match_record ON match_record.id = statistic.match_id
        WHERE match_record.season_id = ? AND statistic.provider = ?
        UNION ALL
        SELECT incident.created_at
        FROM match_incidents incident
        JOIN matches match_record ON match_record.id = incident.match_id
        WHERE match_record.season_id = ? AND incident.provider = ?
      ) freshness_rows
      """;

  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;
  private final Duration staleAfter;

  public DataReadinessService(
      JdbcTemplate jdbcTemplate,
      Clock clock,
      @Value("${app.workflow.data-readiness.stale-after-hours:72}") long staleAfterHours) {
    if (staleAfterHours <= 0) {
      throw new IllegalArgumentException("staleAfterHours must be positive");
    }
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
    this.staleAfter = Duration.ofHours(staleAfterHours);
  }

  public DataReadinessReport inspect(Long seasonId, Provider provider) {
    if (seasonId == null || seasonId <= 0 || provider == null) {
      throw new IllegalArgumentException("Positive seasonId and provider are required");
    }
    Map<String, Object> counts = jdbcTemplate.queryForMap(COUNTS_SQL, seasonId);
    int totalMatches = number(counts.get("total_matches"));
    int finishedMatches = number(counts.get("finished_matches"));
    int finishedWithoutScore = number(counts.get("finished_without_score"));

    int xgMatches = count(XG_COVERAGE_SQL, seasonId, provider.name());
    int lineupMatches = count(LINEUP_COVERAGE_SQL, seasonId, provider.name());
    int incidentMatches = count(INCIDENT_COVERAGE_SQL, seasonId, provider.name());
    int playerStatisticMatches = count(PLAYER_STAT_COVERAGE_SQL, seasonId, provider.name());
    Timestamp timestamp =
        jdbcTemplate.queryForObject(
            FRESHNESS_SQL,
            (result, row) -> result.getTimestamp("latest_update"),
            seasonId,
            seasonId,
            provider.name(),
            seasonId,
            provider.name(),
            seasonId,
            provider.name(),
            seasonId,
            provider.name());
    Instant latestUpdate = timestamp == null ? null : timestamp.toInstant();
    Instant now = Instant.now(clock);
    long ageMinutes =
        latestUpdate == null ? -1 : Math.max(0, Duration.between(latestUpdate, now).toMinutes());
    boolean stale =
        latestUpdate == null || Duration.between(latestUpdate, now).compareTo(staleAfter) > 0;
    boolean consistent = totalMatches > 0 && finishedWithoutScore == 0;
    String status = !consistent ? "NEEDS_ATTENTION" : stale ? "STALE" : "READY";

    return new DataReadinessReport(
        seasonId,
        provider,
        totalMatches,
        finishedMatches,
        finishedWithoutScore,
        percentage(xgMatches, finishedMatches),
        percentage(lineupMatches, finishedMatches),
        percentage(incidentMatches, finishedMatches),
        percentage(playerStatisticMatches, finishedMatches),
        latestUpdate,
        ageMinutes,
        stale,
        consistent,
        status);
  }

  private int count(String sql, Long seasonId, String provider) {
    Integer result = jdbcTemplate.queryForObject(sql, Integer.class, seasonId, provider);
    return result == null ? 0 : result;
  }

  private int number(Object value) {
    return value instanceof Number number ? number.intValue() : 0;
  }

  private BigDecimal percentage(int numerator, int denominator) {
    if (denominator == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  public record DataReadinessReport(
      Long seasonId,
      Provider provider,
      int totalMatches,
      int finishedMatches,
      int finishedMatchesWithoutScore,
      BigDecimal expectedGoalsCoveragePercent,
      BigDecimal confirmedLineupCoveragePercent,
      BigDecimal incidentCoveragePercent,
      BigDecimal playerStatisticsCoveragePercent,
      Instant latestDataUpdateAt,
      long dataAgeMinutes,
      boolean stale,
      boolean consistent,
      String status) {}
}
