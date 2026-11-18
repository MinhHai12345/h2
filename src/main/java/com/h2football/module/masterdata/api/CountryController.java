package com.h2football.module.masterdata.api;

import static com.h2football.module.masterdata.api.MasterDataDtos.*;

import com.h2football.module.masterdata.application.CountryApplicationService;
import com.h2football.module.masterdata.domain.model.Country;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/countries")
public class CountryController {
  private final CountryApplicationService service;

  public CountryController(CountryApplicationService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CountryResponse create(@Valid @RequestBody CountryRequest request) {
    return map(service.create(request.isoCode(), request.name()));
  }

  @GetMapping
  public List<CountryResponse> findAll() {
    return service.findAll().stream().map(this::map).toList();
  }

  @GetMapping("/{id}")
  public CountryResponse get(@PathVariable Long id) {
    return map(service.get(id));
  }

  private CountryResponse map(Country c) {
    return new CountryResponse(c.getId(), c.getIsoCode(), c.getName());
  }
}
