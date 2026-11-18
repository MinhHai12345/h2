package com.h2football.module.provider.sofascore.mapper;

import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import com.h2football.module.provider.sofascore.model.SofaScoreEventSnapshot;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreMatchMapper {
  public SofaScoreEventSnapshot toSnapshot(SofaScoreDtos.Event event) {
    if (event == null
        || event.id() == null
        || event.startTimestamp() == null
        || event.homeTeam() == null
        || event.homeTeam().id() == null
        || event.awayTeam() == null
        || event.awayTeam().id() == null) return null;
    return new SofaScoreEventSnapshot(
        event.id().toString(),
        Instant.ofEpochSecond(event.startTimestamp()),
        event.homeTeam().id().toString(),
        event.awayTeam().id().toString(),
        event.status() == null ? null : event.status().type(),
        event.homeScore() == null ? null : event.homeScore().current(),
        event.awayScore() == null ? null : event.awayScore().current(),
        event.roundInfo() == null ? null : event.roundInfo().name());
  }
}
