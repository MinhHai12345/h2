package com.h2football.module.masterdata.application;

import com.h2football.module.masterdata.domain.model.*;
import com.h2football.module.masterdata.domain.repository.*;
import com.h2football.module.provider.standardization.ProviderDataStandardizer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LeagueApplicationService {
  private final LeagueRepository leagues;
  private final CountryRepository countries;

  public LeagueApplicationService(LeagueRepository leagues, CountryRepository countries) {
    this.leagues = leagues;
    this.countries = countries;
  }

  public League create(String code, String name, Long countryId) {
    String normalizedCode = code.trim().toUpperCase();
    if (leagues.existsByCode(normalizedCode))
      throw new DuplicateMasterDataException("League code already exists: " + normalizedCode);
    Country country =
        countries
            .findById(countryId)
            .orElseThrow(() -> new MasterDataNotFoundException("Country", countryId));
    String canonicalName = ProviderDataStandardizer.leagueName(name);
    return leagues.save(new League(normalizedCode, canonicalName, country));
  }

  @Transactional(readOnly = true)
  public List<League> findAll() {
    return leagues.findAll();
  }

  @Transactional(readOnly = true)
  public League get(Long id) {
    return leagues.findById(id).orElseThrow(() -> new MasterDataNotFoundException("League", id));
  }
}
