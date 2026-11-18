package com.h2football.module.masterdata.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.h2football.module.masterdata.domain.repository.CountryRepository;
import org.junit.jupiter.api.Test;

class CountryApplicationServiceTest {
  @Test
  void shouldNormalizeIsoCodeBeforePersisting() {
    CountryRepository repository = mock(CountryRepository.class);
    when(repository.existsByIsoCode("VN")).thenReturn(false);
    CountryApplicationService service = new CountryApplicationService(repository);

    service.create(" vn ", " Vietnam ");

    verify(repository)
        .save(
            argThat(
                country ->
                    country.getIsoCode().equals("VN") && country.getName().equals("Vietnam")));
  }

  @Test
  void shouldRejectDuplicateIsoCode() {
    CountryRepository repository = mock(CountryRepository.class);
    when(repository.existsByIsoCode("VN")).thenReturn(true);
    CountryApplicationService service = new CountryApplicationService(repository);

    assertThatThrownBy(() -> service.create("vn", "Vietnam"))
        .isInstanceOf(DuplicateMasterDataException.class);
    verify(repository, never()).save(any());
  }
}
