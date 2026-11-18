package com.h2football.module.player.domain.repository;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.player.domain.model.PlayerSeasonStatistic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSeasonStatisticRepository
    extends JpaRepository<PlayerSeasonStatistic, Long> {
  Optional<PlayerSeasonStatistic> findBySeasonIdAndPlayerIdAndProvider(
      Long seasonId, Long playerId, Provider provider);

  @EntityGraph(attributePaths = {"player"})
  List<PlayerSeasonStatistic> findAllBySeasonIdAndProviderOrderByGoalsDescAverageRatingDesc(
      Long seasonId, Provider provider);
}
