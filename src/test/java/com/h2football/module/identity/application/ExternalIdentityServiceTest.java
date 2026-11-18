package com.h2football.module.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.domain.model.ExternalIdentity;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.identity.domain.repository.ExternalIdentityRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExternalIdentityServiceTest {
  private final ExternalIdentityRepository repository = mock(ExternalIdentityRepository.class);
  private final ExternalIdentityService service = new ExternalIdentityService(repository);

  @Test
  void registersAndReusesSameProviderMapping() {
    ExternalIdentity existing =
        new ExternalIdentity(ResourceType.TEAM, Provider.SOFASCORE, "42", 200L);
    when(repository.findByResourceTypeAndProviderAndExternalId(
            ResourceType.TEAM, Provider.SOFASCORE, "42"))
        .thenReturn(Optional.of(existing));

    ExternalIdentity result = service.register(ResourceType.TEAM, Provider.SOFASCORE, "42", 200L);

    assertThat(result.getCanonicalId()).isEqualTo(200L);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsMappingOneExternalResourceToDifferentCanonicalRecords() {
    when(repository.findByResourceTypeAndProviderAndExternalId(
            ResourceType.MATCH, Provider.SOFASCORE, "match-1"))
        .thenReturn(
            Optional.of(
                new ExternalIdentity(ResourceType.MATCH, Provider.SOFASCORE, "match-1", 500L)));

    assertThatThrownBy(
            () -> service.register(ResourceType.MATCH, Provider.SOFASCORE, "match-1", 501L))
        .isInstanceOf(ExternalIdentityConflictException.class);
  }

  @Test
  void allowsDifferentProvidersToResolveToSameCanonicalRecord() {
    when(repository.save(any(ExternalIdentity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ExternalIdentity sofascore =
        service.register(ResourceType.TEAM, Provider.SOFASCORE, "42", 200L);
    ExternalIdentity footballData =
        service.register(ResourceType.TEAM, Provider.FOOTBALL_DATA, "65", 200L);

    assertThat(sofascore.getCanonicalId()).isEqualTo(footballData.getCanonicalId());
    verify(repository, times(2)).save(any(ExternalIdentity.class));
  }
}
