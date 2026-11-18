package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByCountryIdAndNameIgnoreCase(Long countryId, String name);
}