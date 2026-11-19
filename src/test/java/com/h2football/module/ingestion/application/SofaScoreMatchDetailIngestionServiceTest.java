package com.h2football.module.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.ingestion.scheduling.SofaScoreIngestionJobProperties;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SofaScoreMatchDetailIngestionServiceTest {
  private SofaScoreMatchStatisticsIngestionService statistics;
  private SofaScoreMatchLineupIngestionService lineups;
  private SofaScoreMatchIncidentIngestionService incidents;
  private SofaScorePlayerMatchStatisticsIngestionService playerStatistics;
  private SofaScoreIngestionJobProperties properties;
  private SofaScoreMatchDetailIngestionService service;

  @BeforeEach
  void setUp() {
    statistics = mock(SofaScoreMatchStatisticsIngestionService.class);
    lineups = mock(SofaScoreMatchLineupIngestionService.class);
    incidents = mock(SofaScoreMatchIncidentIngestionService.class);
    playerStatistics = mock(SofaScorePlayerMatchStatisticsIngestionService.class);
    properties = new SofaScoreIngestionJobProperties();
    properties.getMatchDetails().setEnabled(true);
    properties.getMatchDetails().setRetryAttempts(2);
    properties.getMatchDetails().setRetryDelayMillis(0);
    service =
        new SofaScoreMatchDetailIngestionService(
            statistics, lineups, incidents, playerStatistics, properties);
  }

  @Test
  void shouldProcessStreamsInDependencyOrderForEveryUniqueEvent() {
    var result = service.sync(Set.of(200L, 100L));

    assertThat(result.matchesProcessed()).isEqualTo(2);
    assertThat(result.statisticsSucceeded()).isEqualTo(2);
    assertThat(result.lineupsSucceeded()).isEqualTo(2);
    assertThat(result.incidentsSucceeded()).isEqualTo(2);
    assertThat(result.playerStatisticsSucceeded()).isEqualTo(2);
    assertThat(result.failures()).isEmpty();

    var inOrder = org.mockito.Mockito.inOrder(statistics, lineups, incidents, playerStatistics);
    inOrder.verify(statistics).sync(100L);
    inOrder.verify(lineups).sync(100L);
    inOrder.verify(incidents).sync(100L);
    inOrder.verify(playerStatistics).sync(100L);
    inOrder.verify(statistics).sync(200L);
    inOrder.verify(lineups).sync(200L);
    inOrder.verify(incidents).sync(200L);
    inOrder.verify(playerStatistics).sync(200L);
  }

  @Test
  void shouldRetryOnlyFailedStreamAndContinueIndependentStreams() {
    when(statistics.sync(100L))
        .thenThrow(new IllegalStateException("temporary provider error"))
        .thenReturn(null);

    var result = service.sync(Set.of(100L));

    assertThat(result.statisticsSucceeded()).isEqualTo(1);
    assertThat(result.lineupsSucceeded()).isEqualTo(1);
    assertThat(result.incidentsSucceeded()).isEqualTo(1);
    assertThat(result.playerStatisticsSucceeded()).isEqualTo(1);
    assertThat(result.failures()).isEmpty();
    verify(statistics, times(2)).sync(100L);
    verify(lineups, times(1)).sync(100L);
    verify(incidents, times(1)).sync(100L);
    verify(playerStatistics, times(1)).sync(100L);
  }

  @Test
  void shouldContinueOtherStreamsWhenStatisticsExhaustsItsOwnRetries() {
    when(statistics.sync(100L)).thenThrow(new IllegalStateException("statistics unavailable"));

    var result = service.sync(Set.of(100L));

    assertThat(result.statisticsSucceeded()).isZero();
    assertThat(result.lineupsSucceeded()).isEqualTo(1);
    assertThat(result.incidentsSucceeded()).isEqualTo(1);
    assertThat(result.playerStatisticsSucceeded()).isEqualTo(1);
    assertThat(result.failures()).hasSize(1);
    verify(statistics, times(2)).sync(100L);
    verify(lineups).sync(100L);
    verify(incidents).sync(100L);
    verify(playerStatistics).sync(100L);
  }

  @Test
  void shouldSkipPlayerStatisticsWhenLineupFailsAfterIndependentRetries() {
    when(lineups.sync(100L)).thenThrow(new IllegalStateException("lineup unavailable"));

    var result = service.sync(Set.of(100L));

    assertThat(result.lineupsSucceeded()).isZero();
    assertThat(result.incidentsSucceeded()).isEqualTo(1);
    assertThat(result.skippedPlayerStatistics()).isEqualTo(1);
    assertThat(result.failures()).hasSize(1);
    verify(lineups, times(2)).sync(100L);
    verify(incidents).sync(100L);
    verify(playerStatistics, never()).sync(100L);
  }

  @Test
  void shouldSkipWhenDetailsAreDisabled() {
    properties.getMatchDetails().setEnabled(false);

    var result = service.sync(Set.of(100L));

    assertThat(result.matchesProcessed()).isZero();
    verify(statistics, never()).sync(100L);
  }
}
