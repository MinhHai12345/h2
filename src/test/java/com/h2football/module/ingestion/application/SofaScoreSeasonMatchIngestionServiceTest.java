package com.h2football.module.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SofaScoreSeasonMatchIngestionServiceTest {
  private SofaScoreMasterDataIngestionService masterData;
  private SofaScoreMatchIngestionService matches;
  private SofaScoreSeasonMatchIngestionService service;

  @BeforeEach
  void setUp() {
    masterData = mock(SofaScoreMasterDataIngestionService.class);
    matches = mock(SofaScoreMatchIngestionService.class);
    service = new SofaScoreSeasonMatchIngestionService(masterData, matches);
  }

  @Test
  void syncImportsTeamsAndTraversesBothEventWindowsUntilAnEmptyPage() {
    when(masterData.syncTeams(17L, 52186L))
        .thenReturn(new SofaScoreMasterDataIngestionService.TeamSyncResult(20, 20, 0, List.of()));

    for (SofaScoreEventWindow window : SofaScoreEventWindow.values()) {
      when(matches.sync(17L, 52186L, 900L, window, 0))
          .thenReturn(
              new SofaScoreMatchIngestionService.MatchSyncResult(
                  2, 2, 0, 0, List.of(1001L, 1002L), List.of()));
      when(matches.sync(17L, 52186L, 900L, window, 1))
          .thenReturn(
              new SofaScoreMatchIngestionService.MatchSyncResult(
                  0, 0, 0, 0, List.of(), List.of()));
    }

    var result = service.sync(17L, 52186L, 900L);

    assertThat(result.teams().importedTeams()).isEqualTo(20);
    assertThat(result.discoveredEvents()).isEqualTo(4);
    assertThat(result.createdMatches()).isEqualTo(4);
    assertThat(result.uniqueEvents()).isEqualTo(2);
    assertThat(result.windows()).hasSize(2);
    assertThat(result.windows()).allSatisfy(window -> assertThat(window.pageLimitReached()).isFalse());
  }

  @Test
  void rejectsNonPositiveIdentifiersBeforeCallingProviders() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.sync(0, 52186L, 900L))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
