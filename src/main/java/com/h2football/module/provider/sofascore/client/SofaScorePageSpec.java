package com.h2football.module.provider.sofascore.client;

import java.util.List;
import java.util.Objects;

public record SofaScorePageSpec(String loadPageUrl, List<SofaScoreEndpointSpec> endpoints) {
  public SofaScorePageSpec {
    if (loadPageUrl == null || loadPageUrl.isBlank()) {
      throw new IllegalArgumentException("SofaScore page URL must not be blank");
    }
    Objects.requireNonNull(endpoints, "SofaScore endpoints are required");
    endpoints = List.copyOf(endpoints);
    if (endpoints.isEmpty() || endpoints.stream().noneMatch(SofaScoreEndpointSpec::required)) {
      throw new IllegalArgumentException("At least one required endpoint must be configured");
    }
  }

  public SofaScoreEndpointSpec requiredEndpoint() {
    return endpoints.stream().filter(SofaScoreEndpointSpec::required).findFirst().orElseThrow();
  }
}
