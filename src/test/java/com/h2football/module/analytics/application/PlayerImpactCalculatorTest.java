package com.h2football.module.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchPlayerStatistic;
import com.h2football.module.match.domain.model.MatchPlayerStatistic.MatchPlayerStatisticSnapshot;
import com.h2football.module.player.domain.model.Player;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerImpactCalculatorTest {
  private PlayerImpactCalculator calculator;
  private Player player;

  @BeforeEach
  void setUp() {
    calculator = new PlayerImpactCalculator();
    player = new Player("Test Player", null, null, "F", null, null);
  }

  @Test
  void calculatesRoleAwareForwardImpactFromRecentObservations() {
    var result =
        calculator.calculate(
            List.of(stat(90, 2, 4, 2, 1, 0, 0, 1, "0.8", "0.2", "7.5")), "F");

    assertThat(result.score()).isNotNull().isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    assertThat(result.attackingIndex()).isNotNull();
    assertThat(result.creationIndex()).isNotNull();
    assertThat(result.matchesObserved()).isEqualTo(1);
    assertThat(result.minutesObserved()).isEqualTo(90);
  }

  @Test
  void returnsMissingScoreWhenNoHistoricalMinutesExist() {
    var result =
        calculator.calculate(
            List.of(stat(0, 0, 0, 0, 0, 0, 0, 0, null, null, null)), "D");

    assertThat(result.score()).isNull();
    assertThat(result.minutesObserved()).isZero();
  }

  @Test
  void rejectsMissingObservations() {
    assertThatThrownBy(() -> calculator.calculate(null, "M"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private MatchPlayerStatistic stat(
      int minutes,
      int goals,
      int shots,
      int keyPasses,
      int tackles,
      int interceptions,
      int clearances,
      int assists,
      String expectedGoals,
      String expectedAssists,
      String rating) {
    return new MatchPlayerStatistic(
        mock(Match.class),
        player,
        Provider.SOFASCORE,
        new MatchPlayerStatisticSnapshot(
            rating == null ? null : new BigDecimal(rating),
            minutes,
            goals,
            assists,
            shots,
            Math.min(shots, 2),
            20,
            25,
            keyPasses,
            tackles,
            interceptions,
            clearances,
            expectedGoals == null ? null : new BigDecimal(expectedGoals),
            expectedAssists == null ? null : new BigDecimal(expectedAssists),
            null));
  }
}
