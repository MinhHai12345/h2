package com.h2football.module.match.api;

import com.h2football.module.match.application.MatchApplicationService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@Validated
public class MatchController {
  private final MatchApplicationService matches;

  public MatchController(MatchApplicationService matches) {
    this.matches = matches;
  }

  @GetMapping
  public List<MatchDtos.MatchResponse> findBySeason(@RequestParam @Positive Long seasonId) {
    return matches.findBySeason(seasonId).stream().map(MatchDtos.MatchResponse::from).toList();
  }

  @GetMapping("/{id}")
  public MatchDtos.MatchResponse get(@PathVariable @Positive Long id) {
    return MatchDtos.MatchResponse.from(matches.get(id));
  }
}
