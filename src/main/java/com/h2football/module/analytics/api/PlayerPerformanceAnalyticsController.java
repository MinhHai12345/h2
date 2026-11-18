package com.h2football.module.analytics.api;

import com.h2football.module.analytics.application.PlayerPerformanceAnalyticsApplicationService;
import com.h2football.module.analytics.application.PlayerPerformanceAnalyticsApplicationService.PlayerPerformanceResponse;
import com.h2football.module.identity.domain.model.Provider;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seasons/{seasonId}/player-performance")
public class PlayerPerformanceAnalyticsController {
  private final PlayerPerformanceAnalyticsApplicationService performance;

  public PlayerPerformanceAnalyticsController(
      PlayerPerformanceAnalyticsApplicationService performance) {
    this.performance = performance;
  }

  @GetMapping
  public List<PlayerPerformanceResponse> find(
      @PathVariable Long seasonId,
      @RequestParam(defaultValue = "SOFASCORE") Provider provider,
      @RequestParam(defaultValue = "0") long minimumMinutes,
      @RequestParam(required = false) String position) {
    return performance.findBySeason(seasonId, provider, minimumMinutes, position);
  }
}
