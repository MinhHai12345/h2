package com.h2football.module.provider.sofascore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SofaScoreStatisticsMapperTest {
  private final SofaScoreStatisticsMapper mapper = new SofaScoreStatisticsMapper();

  @Test
  void mapsExpectedGoalsAndPercentagesAcrossSourcePeriods() {
    var response =
        new SofaScoreDtos.EventStatisticsResponse(
            List.of(
                new SofaScoreDtos.StatisticPeriod(
                    "ALL",
                    List.of(
                        new SofaScoreDtos.StatisticGroup(
                            "Match overview",
                            List.of(
                                new SofaScoreDtos.StatisticItem(
                                    "Expected goals",
                                    "expectedGoals",
                                    "1.86",
                                    "0.71",
                                    new BigDecimal("1.86"),
                                    new BigDecimal("0.71")),
                                new SofaScoreDtos.StatisticItem(
                                    "Ball possession",
                                    "ballPossession",
                                    "61%",
                                    "39%",
                                    null,
                                    null)))))));
    var values = mapper.toSnapshots(response);
    assertThat(values).hasSize(2);
    assertThat(values.get(0).homeValue()).isEqualByComparingTo("1.86");
    assertThat(values.get(1).homeValue()).isEqualByComparingTo("61");
    assertThat(values.get(1).homeDisplay()).isEqualTo("61%");
  }

  @Test
  void doesNotInventNumericValueForRatioDisplay() {
    var response =
        new SofaScoreDtos.EventStatisticsResponse(
            List.of(
                new SofaScoreDtos.StatisticPeriod(
                    "ALL",
                    List.of(
                        new SofaScoreDtos.StatisticGroup(
                            "Passes",
                            List.of(
                                new SofaScoreDtos.StatisticItem(
                                    "Accurate passes",
                                    "accuratePasses",
                                    "12/26 (46%)",
                                    "18/30 (60%)",
                                    null,
                                    null)))))));
    var value = mapper.toSnapshots(response).get(0);
    assertThat(value.homeDisplay()).isEqualTo("12/26 (46%)");
    assertThat(value.homeValue()).isNull();
  }
}
