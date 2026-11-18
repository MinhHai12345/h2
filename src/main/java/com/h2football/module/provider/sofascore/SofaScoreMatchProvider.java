package com.h2football.module.provider.sofascore;

import com.h2football.module.provider.sofascore.client.SofaScoreClient;
import com.h2football.module.provider.sofascore.mapper.SofaScoreMatchMapper;
import com.h2football.module.provider.sofascore.mapper.SofaScoreStatisticsMapper;
import com.h2football.module.provider.sofascore.model.SofaScoreEventSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import com.h2football.module.provider.sofascore.model.SofaScoreStatisticSnapshot;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreMatchProvider {
  private final SofaScoreClient client;
  private final SofaScoreMatchMapper mapper;
  private final SofaScoreStatisticsMapper statisticsMapper;

  public SofaScoreMatchProvider(
      SofaScoreClient client,
      SofaScoreMatchMapper mapper,
      SofaScoreStatisticsMapper statisticsMapper) {
    this.client = client;
    this.mapper = mapper;
    this.statisticsMapper = statisticsMapper;
  }

  public List<SofaScoreEventSnapshot> fetchEvents(
      long tournamentId, long seasonId, SofaScoreEventWindow window, int page) {
    return client.fetchEvents(tournamentId, seasonId, window, page).stream()
        .map(mapper::toSnapshot)
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  public List<SofaScoreStatisticSnapshot> fetchStatistics(long eventId) {
    return statisticsMapper.toSnapshots(client.fetchEventStatistics(eventId));
  }
}
