package com.h2football.module.masterdata.api;

import static com.h2football.module.masterdata.api.MasterDataDtos.*;

import com.h2football.module.masterdata.application.TeamApplicationService;
import com.h2football.module.masterdata.domain.model.Team;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/teams")
public class TeamController {
  private final TeamApplicationService service;

  public TeamController(TeamApplicationService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TeamResponse create(@Valid @RequestBody TeamRequest request) {
    return map(service.create(request.name(), request.countryId()));
  }

  @GetMapping
  public List<TeamResponse> findAll() {
    return service.findAll().stream().map(this::map).toList();
  }

  @GetMapping("/{id}")
  public TeamResponse get(@PathVariable Long id) {
    return map(service.get(id));
  }

  private TeamResponse map(Team t) {
    return new TeamResponse(
        t.getId(), t.getName(), t.getCountry().getId(), t.getCountry().getName());
  }
}
