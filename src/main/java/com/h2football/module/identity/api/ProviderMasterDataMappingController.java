package com.h2football.module.identity.api;

import com.h2football.module.identity.application.MasterDataReconciliationService;
import com.h2football.module.identity.application.MasterDataReconciliationService.ReconciliationCandidate;
import com.h2football.module.identity.application.ProviderMasterDataMappingService;
import com.h2football.module.identity.application.ProviderMasterDataMappingService.MappingCommand;
import com.h2football.module.identity.application.ProviderMasterDataMappingService.MappingResult;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/provider-master-data")
public class ProviderMasterDataMappingController {
  private final ProviderMasterDataMappingService mappings;
  private final MasterDataReconciliationService reconciliation;

  public ProviderMasterDataMappingController(
      ProviderMasterDataMappingService mappings,
      MasterDataReconciliationService reconciliation) {
    this.mappings = mappings;
    this.reconciliation = reconciliation;
  }

  @PostMapping("/mappings")
  @ResponseStatus(HttpStatus.CREATED)
  public MappingResult map(@Valid @RequestBody MappingRequest request) {
    return mappings.map(
        request.provider(), request.resourceType(), request.externalId(), request.canonicalId());
  }

  @PostMapping("/mappings/batch")
  public List<MappingResult> mapBatch(@RequestBody List<@Valid MappingRequest> requests) {
    return mappings.mapAll(
        requests.stream()
            .map(
                item ->
                    new MappingCommand(
                        item.provider(), item.resourceType(), item.externalId(), item.canonicalId()))
            .toList());
  }

  @GetMapping("/reconciliation")
  public List<ReconciliationCandidate> findCandidates(
      @RequestParam(required = false) ResourceType resourceType) {
    return reconciliation.findCandidates(resourceType);
  }

  public record MappingRequest(
      @NotNull Provider provider,
      @NotNull ResourceType resourceType,
      @NotBlank @Size(max = 120) String externalId,
      @NotNull @Positive Long canonicalId) {}
}
