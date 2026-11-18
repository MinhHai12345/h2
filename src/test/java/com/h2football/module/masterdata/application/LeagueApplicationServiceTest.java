package com.h2football.module.masterdata.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.h2football.module.masterdata.domain.repository.CountryRepository;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LeagueApplicationServiceTest {
  @Test
  void shouldRejectUnknownCountry() {
    LeagueRepository leagues = mock(LeagueRepository.class);
    CountryRepository countries = mock(CountryRepository.class);
    when(leagues.existsByCode("EPL")).thenReturn(false);
    when(countries.findById(99L)).thenReturn(Optional.empty());
    LeagueApplicationService service = new LeagueApplicationService(leagues, countries);

    assertThatThrownBy(() -> service.create("epl", "Premier League", 99L))
        .isInstanceOf(MasterDataNotFoundException.class);
    verify(leagues, never()).save(any());
  }

  @Test
  void shouldRejectDuplicateLeagueCode() {
    LeagueRepository leagues = mock(LeagueRepository.class);
    CountryRepository countries = mock(CountryRepository.class);
    when(leagues.existsByCode("EPL")).thenReturn(true);
    LeagueApplicationService service = new LeagueApplicationService(leagues, countries);

    assertThatThrownBy(() -> service.create("epl", "Premier League", 1L))
        .isInstanceOf(DuplicateMasterDataException.class);
    verifyNoInteractions(countries);
  }
}
