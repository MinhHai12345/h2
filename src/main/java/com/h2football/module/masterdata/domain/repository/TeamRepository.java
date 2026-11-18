package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Team;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByCountryIdAndNameIgnoreCase(Long countryId, String name);

    @Override
    @EntityGraph(attributePaths = "country")
    List<Team> findAll();

    @Override
    @EntityGraph(attributePaths = "country")
    Optional<Team> findById(Long id);
}