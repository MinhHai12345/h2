package com.h2football.module.provider.sofascore.mapper;

import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import com.h2football.module.provider.sofascore.model.SofaScoreStatisticSnapshot;
import com.h2football.module.provider.standardization.ProviderDataStandardizer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreStatisticsMapper {
  private static final Pattern NUMBER = Pattern.compile("^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)$");

  public List<SofaScoreStatisticSnapshot> toSnapshots(
      SofaScoreDtos.EventStatisticsResponse response) {
    if (response == null || response.statistics() == null) return List.of();
    return response.statistics().stream()
        .filter(java.util.Objects::nonNull)
        .flatMap(
            period ->
                period.groups() == null
                    ? java.util.stream.Stream.empty()
                    : period.groups().stream()
                        .filter(java.util.Objects::nonNull)
                        .flatMap(
                            group ->
                                group.statisticsItems() == null
                                    ? java.util.stream.Stream.empty()
                                    : group.statisticsItems().stream()
                                        .filter(java.util.Objects::nonNull)
                                        .map(item -> map(period, group, item))
                                        .filter(java.util.Objects::nonNull)))
        .toList();
  }

  private SofaScoreStatisticSnapshot map(
      SofaScoreDtos.StatisticPeriod period,
      SofaScoreDtos.StatisticGroup group,
      SofaScoreDtos.StatisticItem item) {
    String p = clean(period.period()), g = clean(group.groupName());
    String sourceKey = clean(item.key()), name = clean(item.name());
    String key = ProviderDataStandardizer.statisticKey(sourceKey, name);
    if (p == null || g == null || key == null || name == null) return null;
    return new SofaScoreStatisticSnapshot(
        p.toUpperCase(Locale.ROOT),
        g,
        key,
        name,
        clean(item.home()),
        clean(item.away()),
        numeric(item.homeValue(), item.home()),
        numeric(item.awayValue(), item.away()));
  }

  private BigDecimal numeric(BigDecimal source, String display) {
    if (source != null) return source;
    String value = clean(display);
    if (value == null) return null;
    String normalized = value.endsWith("%") ? value.substring(0, value.length() - 1).trim() : value;
    if (!NUMBER.matcher(normalized).matches()) return null;
    try {
      return new BigDecimal(normalized);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
