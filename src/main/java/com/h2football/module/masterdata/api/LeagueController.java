package com.h2football.module.masterdata.api;

import com.h2football.module.masterdata.application.LeagueApplicationService;
import com.h2football.module.masterdata.domain.model.League;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.h2football.module.masterdata.api.MasterDataDtos.*;

@RestController
@RequestMapping("/api/v1/master-data/leagues")
public class LeagueController {
    private final LeagueApplicationService service;
    public LeagueController(LeagueApplicationService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public LeagueResponse create(@Valid @RequestBody LeagueRequest request) {
        return map(service.create(request.code(), request.name(), request.countryId()));
    }
    @GetMapping public List<LeagueResponse> findAll() { return service.findAll().stream().map(this::map).toList(); }
    @GetMapping("/{id}") public LeagueResponse get(@PathVariable Long id) { return map(service.get(id)); }
    private LeagueResponse map(League l) { return new LeagueResponse(l.getId(), l.getCode(), l.getName(), l.getCountry().getId(), l.getCountry().getName()); }
}