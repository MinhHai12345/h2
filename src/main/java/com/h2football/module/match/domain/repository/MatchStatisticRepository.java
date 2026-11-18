package com.h2football.module.match.domain.repository;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.MatchStatistic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchStatisticRepository extends JpaRepository<MatchStatistic, Long> {
  List<MatchStatistic> findAllByMatchIdOrderByPeriodAscGroupNameAscStatisticKeyAsc(Long matchId);

  void deleteAllByMatchIdAndProvider(Long matchId, Provider provider);
}
