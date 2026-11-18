package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.League;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Long> {
    boolean existsByCode(String code);
}