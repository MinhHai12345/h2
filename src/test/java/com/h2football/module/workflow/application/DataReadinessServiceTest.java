package com.h2football.module.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class DataReadinessServiceTest {
  @Mock private JdbcTemplate jdbcTemplate;

  private Clock clock;
  private DataReadinessService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);
    service = new DataReadinessService(jdbcTemplate, clock, 72);
  }

  @Test
  void shouldReportCoverageConsistencyAndFreshnessForSeason() {
    when(jdbcTemplate.queryForMap(anyString(), eq(42L)))
        .thenReturn(
            Map.of(
                "total_matches", 10L,
                "finished_matches", 8L,
                "finished_without_score", 0L));
    doReturn(6, 4, 5, 3)
        .when(jdbcTemplate)
        .queryForObject(anyString(), eq(Integer.class), any(Object[].class));
    doReturn(Timestamp.from(Instant.parse("2026-11-19T09:00:00Z")))
        .when(jdbcTemplate)
        .queryForObject(
            anyString(), org.mockito.ArgumentMatchers.<RowMapper<Timestamp>>any(), any(Object[].class));

    var report = service.inspect(42L, com.h2football.module.identity.domain.model.Provider.SOFASCORE);

    assertThat(report.status()).isEqualTo("READY");
    assertThat(report.consistent()).isTrue();
    assertThat(report.stale()).isFalse();
    assertThat(report.totalMatches()).isEqualTo(10);
    assertThat(report.expectedGoalsCoveragePercent()).hasToString("75.00");
    assertThat(report.confirmedLineupCoveragePercent()).hasToString("50.00");
    assertThat(report.incidentCoveragePercent()).hasToString("62.50");
    assertThat(report.playerStatisticsCoveragePercent()).hasToString("37.50");
    assertThat(report.dataAgeMinutes()).isEqualTo(60);
  }

  @Test
  void shouldFlagMissingScoresAndStaleDataWithoutDiscardingCoverageMetrics() {
    when(jdbcTemplate.queryForMap(anyString(), eq(42L)))
        .thenReturn(
            Map.of(
                "total_matches", 10L,
                "finished_matches", 8L,
                "finished_without_score", 1L));
    doReturn(4, 3, 2, 1)
        .when(jdbcTemplate)
        .queryForObject(anyString(), eq(Integer.class), any(Object[].class));
    doReturn(Timestamp.from(Instant.parse("2026-11-15T00:00:00Z")))
        .when(jdbcTemplate)
        .queryForObject(
            anyString(), org.mockito.ArgumentMatchers.<RowMapper<Timestamp>>any(), any(Object[].class));

    var report = service.inspect(42L, com.h2football.module.identity.domain.model.Provider.SOFASCORE);

    assertThat(report.status()).isEqualTo("NEEDS_ATTENTION");
    assertThat(report.consistent()).isFalse();
    assertThat(report.stale()).isTrue();
    assertThat(report.finishedMatchesWithoutScore()).isEqualTo(1);
  }
}
