package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Season;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    boolean existsByLeagueIdAndSeasonCode(Long leagueId, String seasonCode);

    @EntityGraph(attributePaths = "league")
    List<Season> findAllByLeagueIdOrderByStartDateDesc(Long leagueId);

    @Override
    @EntityGraph(attributePaths = "league")
    List<Season> findAll();

    @Override
    @EntityGraph(attributePaths = "league")
    Optional<Season> findById(Long id);
}