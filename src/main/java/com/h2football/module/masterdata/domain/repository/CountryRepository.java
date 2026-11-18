package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {
    boolean existsByIsoCode(String isoCode);
    Optional<Country> findByIsoCode(String isoCode);
}