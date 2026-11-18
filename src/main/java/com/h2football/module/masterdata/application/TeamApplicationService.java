package com.h2football.module.masterdata.application;

import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.CountryRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TeamApplicationService {
    private final TeamRepository teams;
    private final CountryRepository countries;
    public TeamApplicationService(TeamRepository teams, CountryRepository countries) {
        this.teams = teams; this.countries = countries;
    }
    public Team create(String name, Long countryId) {
        Country country = countries.findById(countryId)
                .orElseThrow(() -> new MasterDataNotFoundException("Country", countryId));
        String normalizedName = name.trim();
        if (teams.existsByCountryIdAndNameIgnoreCase(countryId, normalizedName))
            throw new DuplicateMasterDataException("Team already exists in country: " + normalizedName);
        return teams.save(new Team(normalizedName, country));
    }
    @Transactional(readOnly = true)
    public List<Team> findAll() { return teams.findAll(); }
    @Transactional(readOnly = true)
    public Team get(Long id) { return teams.findById(id).orElseThrow(() -> new MasterDataNotFoundException("Team", id)); }
}