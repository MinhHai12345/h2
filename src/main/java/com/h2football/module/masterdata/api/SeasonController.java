package com.h2football.module.masterdata.api;

import com.h2football.module.masterdata.application.SeasonApplicationService;
import com.h2football.module.masterdata.domain.model.Season;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.h2football.module.masterdata.api.MasterDataDtos.*;

@RestController
@RequestMapping("/api/v1/master-data/seasons")
public class SeasonController {
    private final SeasonApplicationService service;
    public SeasonController(SeasonApplicationService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SeasonResponse create(@Valid @RequestBody SeasonRequest request) {
        return map(service.create(request.leagueId(), request.seasonCode(), request.displayName(), request.startDate(), request.endDate()));
    }
    @GetMapping public List<SeasonResponse> findAll(@RequestParam(required = false) Long leagueId) {
        return service.findAll(leagueId).stream().map(this::map).toList();
    }
    @GetMapping("/{id}") public SeasonResponse get(@PathVariable Long id) { return map(service.get(id)); }
    private SeasonResponse map(Season s) { return new SeasonResponse(s.getId(), s.getLeague().getId(), s.getSeasonCode(), s.getDisplayName(), s.getStartDate(), s.getEndDate()); }
}