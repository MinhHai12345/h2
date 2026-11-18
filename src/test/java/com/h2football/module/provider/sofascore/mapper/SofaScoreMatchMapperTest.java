package com.h2football.module.provider.sofascore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SofaScoreMatchMapperTest {
  private final SofaScoreMatchMapper mapper = new SofaScoreMatchMapper();

  @Test
  void mapsFinishedEventAndCurrentScore() {
    SofaScoreDtos.Event event =
        new SofaScoreDtos.Event(
            991L,
            1_700_000_000L,
            new SofaScoreDtos.Team(10L, "Home", null, null, null),
            new SofaScoreDtos.Team(20L, "Away", null, null, null),
            new SofaScoreDtos.EventStatus("finished", "Finished", "finished"),
            new SofaScoreDtos.Score(2, null),
            new SofaScoreDtos.Score(1, null),
            new SofaScoreDtos.RoundInfo("Round 12"));

    var snapshot = mapper.toSnapshot(event);

    assertThat(snapshot.externalId()).isEqualTo("991");
    assertThat(snapshot.kickoffAt()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
    assertThat(snapshot.statusCode()).isEqualTo("finished");
    assertThat(snapshot.homeScore()).isEqualTo(2);
    assertThat(snapshot.awayScore()).isEqualTo(1);
    assertThat(snapshot.roundName()).isEqualTo("Round 12");
  }

  @Test
  void ignoresMalformedEvents() {
    assertThat(
            mapper.toSnapshot(
                new SofaScoreDtos.Event(null, 1_700_000_000L, null, null, null, null, null, null)))
        .isNull();
  }
}
