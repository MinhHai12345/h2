package com.h2football.module.masterdata.application;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.model.League;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.CountryRepository;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MasterDataImportApplicationService {
  private final CountryRepository countries;
  private final LeagueRepository leagues;
  private final TeamRepository teams;
  private final ExternalIdentityService identities;

  public MasterDataImportApplicationService(
      CountryRepository countries,
      LeagueRepository leagues,
      TeamRepository teams,
      ExternalIdentityService identities) {
    this.countries = countries;
    this.leagues = leagues;
    this.teams = teams;
    this.identities = identities;
  }

  public Country importCountry(Provider provider, String externalId, String isoCode, String name) {
    String sourceId = normalize(externalId, "externalId");
    String code = normalize(isoCode, "isoCode").toUpperCase();
    String countryName = normalize(name, "name");

    Long canonicalId =
        identities.findCanonicalId(ResourceType.COUNTRY, provider, sourceId).orElse(null);
    if (canonicalId != null) {
      return countries
          .findById(canonicalId)
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "Country identity references missing canonical ID " + canonicalId));
    }

    Country country =
        countries
            .findByIsoCode(code)
            .orElseGet(() -> countries.save(new Country(code, countryName)));
    identities.register(ResourceType.COUNTRY, provider, sourceId, country.getId());
    return country;
  }

  public League importLeague(
      Provider provider, String externalId, String code, String name, Long countryId) {
    String sourceId = normalize(externalId, "externalId");
    String normalizedCode = normalize(code, "code").toUpperCase();
    String leagueName = ProviderDataStandardizer.leagueName(normalize(name, "name"));

    Long canonicalId =
        identities.findCanonicalId(ResourceType.LEAGUE, provider, sourceId).orElse(null);
    if (canonicalId != null) {
      return leagues
          .findById(canonicalId)
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "League identity references missing canonical ID " + canonicalId));
    }

    Country country =
        countries
            .findById(countryId)
            .orElseThrow(() -> new MasterDataNotFoundException("Country", countryId));
    League league =
        leagues
            .findByCode(normalizedCode)
            .orElseGet(() -> leagues.save(new League(normalizedCode, leagueName, country)));
    identities.register(ResourceType.LEAGUE, provider, sourceId, league.getId());
    return league;
  }

  public Team importTeam(Provider provider, String externalId, String name, Long countryId) {
    String sourceId = normalize(externalId, "externalId");
    String teamName = normalize(name, "name");

    Long canonicalId =
        identities.findCanonicalId(ResourceType.TEAM, provider, sourceId).orElse(null);
    if (canonicalId != null) {
      return teams
          .findById(canonicalId)
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "Team identity references missing canonical ID " + canonicalId));
    }

    Country country =
        countries
            .findById(countryId)
            .orElseThrow(() -> new MasterDataNotFoundException("Country", countryId));
    Team team =
        teams
            .findByCountryIdAndNameIgnoreCase(countryId, teamName)
            .orElseGet(() -> teams.save(new Team(teamName, country)));
    identities.register(ResourceType.TEAM, provider, sourceId, team.getId());
    return team;
  }

  private String normalize(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }
}
