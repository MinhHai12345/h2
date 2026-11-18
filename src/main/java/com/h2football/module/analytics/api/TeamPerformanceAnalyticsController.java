package com.h2football.module.analytics.api;

import com.h2football.module.analytics.application.TeamPerformanceAnalyticsApplicationService;
import com.h2football.module.analytics.application.TeamPerformanceAnalyticsApplicationService.TeamPerformanceResponse;
import com.h2football.module.identity.domain.model.Provider;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seasons/{seasonId}/team-performance")
public class TeamPerformanceAnalyticsController {
  private final TeamPerformanceAnalyticsApplicationService performance;

  public TeamPerformanceAnalyticsController(
      TeamPerformanceAnalyticsApplicationService performance) {
    this.performance = performance;
  }

  @GetMapping
  public List<TeamPerformanceResponse> findBySeason(
      @PathVariable Long seasonId, @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return performance.findBySeason(seasonId, provider);
  }

  @GetMapping("/{teamId}")
  public TeamPerformanceResponse findByTeam(
      @PathVariable Long seasonId,
      @PathVariable Long teamId,
      @RequestParam(defaultValue = "SOFASCORE") Provider provider) {
    return performance.findByTeam(seasonId, teamId, provider);
  }
}
