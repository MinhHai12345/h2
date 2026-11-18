package com.h2football.module.masterdata.application;

import static org.mockito.Mockito.*;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.repository.CountryRepository;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MasterDataImportApplicationServiceTest {
    private final CountryRepository countries = mock(CountryRepository.class);
    private final LeagueRepository leagues = mock(LeagueRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
    private final MasterDataImportApplicationService service =
            new MasterDataImportApplicationService(countries, leagues, teams, identities);

    @Test
    void reusesCanonicalCountryWhenProviderIdentityAlreadyExists() {
        Country country = mock(Country.class);
        when(identities.findCanonicalId(any(), eq(Provider.SOFASCORE), eq("GB")))
                .thenReturn(Optional.of(9L));
        when(countries.findById(9L)).thenReturn(Optional.of(country));

        service.importCountry(Provider.SOFASCORE, "GB", "gb", "England");

        verify(countries, never()).save(any(Country.class));
        verify(identities, never()).register(any(), any(), anyString(), anyLong());
    }
}
