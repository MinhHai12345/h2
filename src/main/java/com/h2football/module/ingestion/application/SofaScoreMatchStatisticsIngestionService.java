package com.h2football.module.ingestion.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.match.application.MatchStatisticsApplicationService;
import com.h2football.module.match.application.MatchStatisticsApplicationService.StatisticInput;
import com.h2football.module.provider.sofascore.SofaScoreMatchProvider;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SofaScoreMatchStatisticsIngestionService {
  private static final Provider PROVIDER = Provider.SOFASCORE;
  private final SofaScoreMatchProvider provider;
  private final ExternalIdentityService identities;
  private final MatchStatisticsApplicationService statistics;

  public SofaScoreMatchStatisticsIngestionService(
      SofaScoreMatchProvider provider,
      ExternalIdentityService identities,
      MatchStatisticsApplicationService statistics) {
    this.provider = provider;
    this.identities = identities;
    this.statistics = statistics;
  }

  public StatisticsSyncResult sync(long sofaScoreEventId) {
    if (sofaScoreEventId <= 0)
      throw new IllegalArgumentException("sofaScoreEventId must be positive");
    Long matchId =
        identities
            .findCanonicalId(ResourceType.MATCH, PROVIDER, Long.toString(sofaScoreEventId))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "SofaScore event is not mapped; sync matches before statistics"));
    var snapshots = provider.fetchStatistics(sofaScoreEventId);
    List<StatisticInput> inputs =
        snapshots.stream()
            .map(
                item ->
                    new StatisticInput(
                        item.period(),
                        item.groupName(),
                        item.statisticKey(),
                        item.statisticName(),
                        item.homeDisplay(),
                        item.awayDisplay(),
                        item.homeValue(),
                        item.awayValue()))
            .toList();
    int stored = statistics.replaceFromProvider(matchId, PROVIDER, inputs);
    return new StatisticsSyncResult(sofaScoreEventId, matchId, snapshots.size(), stored);
  }

  public record StatisticsSyncResult(
      long sofaScoreEventId,
      long canonicalMatchId,
      int discoveredStatistics,
      int storedStatistics) {}
}
