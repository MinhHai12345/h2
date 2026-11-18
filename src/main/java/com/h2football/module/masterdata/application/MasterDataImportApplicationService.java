package com.h2football.module.masterdata.application;

import com.h2football.module.masterdata.domain.model.*;
import com.h2football.module.masterdata.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MasterDataImportApplicationService {
    private static final String COUNTRY = "COUNTRY";
    private static final String LEAGUE = "LEAGUE";
    private static final String TEAM = "TEAM";

    private final CountryRepository countries;
    private final LeagueRepository leagues;
    private final TeamRepository teams;
    private final MasterDataExternalIdentityRepository identities;

    public MasterDataImportApplicationService(CountryRepository countries, LeagueRepository leagues,
            TeamRepository teams, MasterDataExternalIdentityRepository identities) {
        this.countries = countries;
        this.leagues = leagues;
        this.teams = teams;
        this.identities = identities;
    }

    public Country importCountry(String provider, String externalId, String isoCode, String name) {
        String source = normalize(provider, "provider").toUpperCase();
        String sourceId = normalize(externalId, "externalId");
        String code = normalize(isoCode, "isoCode").toUpperCase();
        String countryName = normalize(name, "name");
        MasterDataExternalIdentity identity = findIdentity(COUNTRY, source, sourceId);
        if (identity != null) {
            return countries.findById(identity.getCanonicalId()).orElseThrow(() ->
                    new IllegalStateException("Country identity references missing canonical ID " + identity.getCanonicalId()));
        }
        Country country = countries.findByIsoCode(code)
                .orElseGet(() -> countries.save(new Country(code, countryName)));
        saveIdentity(COUNTRY, source, sourceId, country.getId());
        return country;
    }

    public League importLeague(String provider, String externalId, String code, String name, Long countryId) {
        String source = normalize(provider, "provider").toUpperCase();
        String sourceId = normalize(externalId, "externalId");
        String normalizedCode = normalize(code, "code").toUpperCase();
        String leagueName = normalize(name, "name");
        MasterDataExternalIdentity identity = findIdentity(LEAGUE, source, sourceId);
        if (identity != null) {
            return leagues.findById(identity.getCanonicalId()).orElseThrow(() ->
                    new IllegalStateException("League identity references missing canonical ID " + identity.getCanonicalId()));
        }
        Country country = countries.findById(countryId)
                .orElseThrow(() -> new MasterDataNotFoundException("Country", countryId));
        League league = leagues.findByCode(normalizedCode)
                .orElseGet(() -> leagues.save(new League(normalizedCode, leagueName, country)));
        saveIdentity(LEAGUE, source, sourceId, league.getId());
        return league;
    }

    public Team importTeam(String provider, String externalId, String name, Long countryId) {
        String source = normalize(provider, "provider").toUpperCase();
        String sourceId = normalize(externalId, "externalId");
        String teamName = normalize(name, "name");
        MasterDataExternalIdentity identity = findIdentity(TEAM, source, sourceId);
        if (identity != null) {
            return teams.findById(identity.getCanonicalId()).orElseThrow(() ->
                    new IllegalStateException("Team identity references missing canonical ID " + identity.getCanonicalId()));
        }
        Country country = countries.findById(countryId)
                .orElseThrow(() -> new MasterDataNotFoundException("Country", countryId));
        Team team = teams.findByCountryIdAndNameIgnoreCase(countryId, teamName)
                .orElseGet(() -> teams.save(new Team(teamName, country)));
        saveIdentity(TEAM, source, sourceId, team.getId());
        return team;
    }

    private MasterDataExternalIdentity findIdentity(String type, String provider, String externalId) {
        return identities.findByResourceTypeAndProviderAndExternalId(type, provider, externalId).orElse(null);
    }

    private void saveIdentity(String type, String provider, String externalId, Long canonicalId) {
        identities.save(new MasterDataExternalIdentity(type, provider, externalId, canonicalId));
    }

    private String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
