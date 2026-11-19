package com.h2football.module.analytics.api;

import com.h2football.module.analytics.application.PlayerImpactApplicationService;
import com.h2football.module.analytics.application.PlayerImpactApplicationService.MatchPlayerImpactResponse;
import com.h2football.module.identity.domain.model.Provider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/player-impact")
public class PlayerImpactController {
  private final PlayerImpactApplicationService playerImpact;

  public PlayerImpactController(PlayerImpactApplicationService playerImpact) {
    this.playerImpact = playerImpact;
  }

  @GetMapping
  public MatchPlayerImpactResponse calculate(
      @PathVariable Long matchId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return playerImpact.calculate(matchId, provider);
  }
}
