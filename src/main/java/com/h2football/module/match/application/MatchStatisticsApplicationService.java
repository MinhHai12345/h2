package com.h2football.module.match.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchStatistic;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.match.domain.repository.MatchStatisticRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchStatisticsApplicationService {
  private final MatchRepository matches;
  private final MatchStatisticRepository statistics;

  public MatchStatisticsApplicationService(
      MatchRepository matches, MatchStatisticRepository statistics) {
    this.matches = matches;
    this.statistics = statistics;
  }

  public int replaceFromProvider(Long matchId, Provider provider, List<StatisticInput> snapshots) {
    if (matchId == null || matchId <= 0)
      throw new IllegalArgumentException("matchId must be positive");
    if (provider == null) throw new IllegalArgumentException("provider must not be null");
    if (!matches.existsById(matchId)) throw new MatchNotFoundException(matchId);
    if (snapshots == null || snapshots.isEmpty()) return 0;

    Map<String, StatisticInput> unique = new LinkedHashMap<>();
    for (StatisticInput item : snapshots) {
      if (item == null) continue;
      unique.put(
          required(item.period(), "period") + "::" + required(item.statisticKey(), "statisticKey"),
          item);
    }
    if (unique.isEmpty()) return 0;
    var match = matches.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    List<MatchStatistic> replacement =
        unique.values().stream()
            .map(
                item ->
                    new MatchStatistic(
                        match,
                        provider,
                        item.period(),
                        item.groupName(),
                        item.statisticKey(),
                        item.statisticName(),
                        item.homeDisplay(),
                        item.awayDisplay(),
                        item.homeValue(),
                        item.awayValue()))
            .toList();
    statistics.deleteAllByMatchIdAndProvider(matchId, provider);
    statistics.saveAll(replacement);
    return replacement.size();
  }

  @Transactional(readOnly = true)
  public List<MatchStatistic> findByMatch(Long matchId) {
    if (matchId == null || matchId <= 0)
      throw new IllegalArgumentException("matchId must be positive");
    if (!matches.existsById(matchId)) throw new MatchNotFoundException(matchId);
    return statistics.findAllByMatchIdOrderByPeriodAscGroupNameAscStatisticKeyAsc(matchId);
  }

  private String required(String value, String field) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException(field + " must not be blank");
    return value.trim();
  }

  public record StatisticInput(
      String period,
      String groupName,
      String statisticKey,
      String statisticName,
      String homeDisplay,
      String awayDisplay,
      BigDecimal homeValue,
      BigDecimal awayValue) {}
}
