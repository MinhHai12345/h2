package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    boolean existsByLeagueIdAndSeasonCode(Long leagueId, String seasonCode);
    List<Season> findAllByLeagueIdOrderByStartDateDesc(Long leagueId);
}