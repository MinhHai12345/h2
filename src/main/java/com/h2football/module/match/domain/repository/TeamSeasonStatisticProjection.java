package com.h2football.module.match.domain.repository;

import java.math.BigDecimal;

public interface TeamSeasonStatisticProjection {
  Long getTeamId();

  String getPeriod();

  String getGroupName();

  String getStatisticKey();

  String getStatisticName();

  Long getMatchesCount();

  BigDecimal getTotalValue();

  BigDecimal getAverageValue();
}
