package com.h2football.module.provider.sofascore.model;

import java.util.Map;

public record SofaScorePlayerStatisticsSnapshot(String rawJson, Map<String, String> values) {
  public SofaScorePlayerStatisticsSnapshot {
    values = values == null ? Map.of() : Map.copyOf(values);
  }

  public String value(String key) {
    return values.get(key);
  }
}
