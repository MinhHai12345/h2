package com.h2football.module.provider.sofascore;

import com.h2football.module.provider.sofascore.client.SofaScoreClient;
import com.h2football.module.provider.sofascore.mapper.SofaScoreMatchMapper;
import com.h2football.module.provider.sofascore.model.SofaScoreEventSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreMatchProvider {
  private final SofaScoreClient client;
  private final SofaScoreMatchMapper mapper;

  public SofaScoreMatchProvider(SofaScoreClient client, SofaScoreMatchMapper mapper) {
    this.client = client;
    this.mapper = mapper;
  }

  public List<SofaScoreEventSnapshot> fetchEvents(
      long tournamentId, long seasonId, SofaScoreEventWindow window, int page) {
    return client.fetchEvents(tournamentId, seasonId, window, page).stream()
        .map(mapper::toSnapshot)
        .filter(java.util.Objects::nonNull)
        .toList();
  }
}
