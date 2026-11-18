package com.h2football.module.provider.sofascore;

import com.h2football.module.provider.sofascore.client.SofaScoreClient;
import com.h2football.module.provider.sofascore.mapper.SofaScoreMatchEventMapper;
import com.h2football.module.provider.sofascore.model.SofaScoreIncidentSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreLineupSnapshot;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreMatchEventProvider {
  private final SofaScoreClient client;
  private final SofaScoreMatchEventMapper mapper;

  public SofaScoreMatchEventProvider(SofaScoreClient client, SofaScoreMatchEventMapper mapper) {
    this.client = client;
    this.mapper = mapper;
  }

  public SofaScoreLineupSnapshot fetchLineups(long eventId) {
    return mapper.toLineup(client.fetchEventLineups(eventId));
  }

  public List<SofaScoreIncidentSnapshot> fetchIncidents(long eventId) {
    return mapper.toIncidents(eventId, client.fetchEventIncidents(eventId));
  }
}
