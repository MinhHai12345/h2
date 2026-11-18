package com.h2football.module.match.domain.repository;

import java.math.BigDecimal;

public interface SeasonPlayerStatisticProjection {
  Long getPlayerId();

  String getPlayerName();

  Long getAppearances();

  Long getMinutesPlayed();

  Long getGoals();

  Long getAssists();

  Long getTotalShots();

  Long getShotsOnTarget();

  Long getAccuratePasses();

  Long getTotalPasses();

  Long getKeyPasses();

  Long getTackles();

  Long getInterceptions();

  Long getClearances();

  BigDecimal getExpectedGoals();

  BigDecimal getExpectedAssists();

  Double getAverageRating();
}
