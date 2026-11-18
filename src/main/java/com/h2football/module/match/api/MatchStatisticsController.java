package com.h2football.module.match.api;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.application.MatchStatisticsApplicationService;
import com.h2football.module.match.domain.model.MatchStatistic;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/statistics")
@Validated
public class MatchStatisticsController {
  private final MatchStatisticsApplicationService statistics;

  public MatchStatisticsController(MatchStatisticsApplicationService statistics) {
    this.statistics = statistics;
  }

  @GetMapping
  public List<MatchStatisticResponse> findByMatch(@PathVariable @Positive Long matchId) {
    return statistics.findByMatch(matchId).stream().map(MatchStatisticResponse::from).toList();
  }

  public record MatchStatisticResponse(
      Long id,
      Provider provider,
      String period,
      String groupName,
      String statisticKey,
      String statisticName,
      String homeDisplay,
      String awayDisplay,
      BigDecimal homeValue,
      BigDecimal awayValue) {
    static MatchStatisticResponse from(MatchStatistic item) {
      return new MatchStatisticResponse(
          item.getId(),
          item.getProvider(),
          item.getPeriod(),
          item.getGroupName(),
          item.getStatisticKey(),
          item.getStatisticName(),
          item.getHomeDisplay(),
          item.getAwayDisplay(),
          item.getHomeValue(),
          item.getAwayValue());
    }
  }
}
