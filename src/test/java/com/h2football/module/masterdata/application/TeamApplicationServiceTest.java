package com.h2football.module.masterdata.application;

import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.repository.CountryRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TeamApplicationServiceTest {
    @Test
    void shouldRejectDuplicateTeamWithinCountry() {
        TeamRepository teams = mock(TeamRepository.class);
        CountryRepository countries = mock(CountryRepository.class);
        when(countries.findById(1L)).thenReturn(Optional.of(mock(Country.class)));
        when(teams.existsByCountryIdAndNameIgnoreCase(1L, "Arsenal")).thenReturn(true);
        TeamApplicationService service = new TeamApplicationService(teams, countries);

        assertThatThrownBy(() -> service.create("Arsenal", 1L))
                .isInstanceOf(DuplicateMasterDataException.class);
        verify(teams, never()).save(any());
    }
}