package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.League;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {
    boolean existsByCode(String code);

    @Override
    @EntityGraph(attributePaths = "country")
    List<League> findAll();

    @Override
    @EntityGraph(attributePaths = "country")
    Optional<League> findById(Long id);
}