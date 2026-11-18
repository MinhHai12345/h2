package com.h2football.module.masterdata.application;

import static org.mockito.Mockito.*;

import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.model.MasterDataExternalIdentity;
import com.h2football.module.masterdata.domain.repository.CountryRepository;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.MasterDataExternalIdentityRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MasterDataImportApplicationServiceTest {
    private final CountryRepository countries = mock(CountryRepository.class);
    private final LeagueRepository leagues = mock(LeagueRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final MasterDataExternalIdentityRepository identities =
            mock(MasterDataExternalIdentityRepository.class);
    private final MasterDataImportApplicationService service =
            new MasterDataImportApplicationService(countries, leagues, teams, identities);

    @Test
    void reusesCanonicalCountryWhenProviderIdentityAlreadyExists() {
        Country country = mock(Country.class);
        when(identities.findByResourceTypeAndProviderAndExternalId("COUNTRY", "SOFASCORE", "GB"))
                .thenReturn(Optional.of(new MasterDataExternalIdentity("COUNTRY", "SOFASCORE", "GB", 9L)));
        when(countries.findById(9L)).thenReturn(Optional.of(country));

        service.importCountry("SofaScore", "GB", "gb", "England");

        verify(countries, never()).save(any(Country.class));
        verify(identities, never()).save(any(MasterDataExternalIdentity.class));
    }
}
