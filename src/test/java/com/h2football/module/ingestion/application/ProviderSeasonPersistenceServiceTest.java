package com.h2football.module.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.match.application.MatchStatisticsApplicationService;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.repository.MatchRepository;
import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.model.League;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.provider.MultiProviderSeasonDataClient.ProviderMatchRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderSeasonPersistenceServiceTest {
  @Mock private SeasonRepository seasons;
  @Mock private TeamRepository teams;
  @Mock private MatchRepository matches;
  @Mock private ExternalIdentityService identities;
  @Mock private MatchStatisticsApplicationService statistics;
  @Mock private Season season;
  @Mock private League league;
  @Mock private Country country;
  @Mock private Team home;
  @Mock private Team away;
  @Mock private Match savedMatch;

  @Test
  void shouldPersistProviderMatchAndRegisterCanonicalIdentities() {
    when(seasons.findById(20L)).thenReturn(Optional.of(season));
    when(season.getLeague()).thenReturn(league);
    when(league.getCountry()).thenReturn(country);
    when(teams.findAll()).thenReturn(List.of());
    when(teams.save(any(Team.class))).thenReturn(home, away);
    when(home.getId()).thenReturn(101L);
    when(away.getId()).thenReturn(102L);
    when(identities.findCanonicalId(ResourceType.TEAM, Provider.FOOTBALL_DATA, "home-1"))
        .thenReturn(Optional.empty());
    when(identities.findCanonicalId(ResourceType.TEAM, Provider.FOOTBALL_DATA, "away-2"))
        .thenReturn(Optional.empty());
    when(identities.findCanonicalId(ResourceType.MATCH, Provider.FOOTBALL_DATA, "match-3"))
        .thenReturn(Optional.empty());
    when(matches.save(any(Match.class))).thenReturn(savedMatch);
    when(savedMatch.getId()).thenReturn(300L);

    var service =
        new ProviderSeasonPersistenceService(seasons, teams, matches, identities, statistics);
    var result =
        service.persist(
            Provider.FOOTBALL_DATA,
            20L,
            List.of(
                new ProviderMatchRecord(
                    "match-3",
                    "home-1",
                    "Home Club",
                    "away-2",
                    "Away Club",
                    Instant.parse("2024-08-16T19:00:00Z"),
                    "FINISHED",
                    2,
                    1,
                    "1",
                    null,
                    null)));

    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isZero();
    assertThat(result.skipped()).isZero();
    verify(identities).register(ResourceType.TEAM, Provider.FOOTBALL_DATA, "home-1", 101L);
    verify(identities).register(ResourceType.TEAM, Provider.FOOTBALL_DATA, "away-2", 102L);
    verify(identities).register(ResourceType.MATCH, Provider.FOOTBALL_DATA, "match-3", 300L);
    verify(matches, never()).findById(300L);
    verify(statistics, never()).replaceFromProvider(any(), any(), any());
  }

  @Test
  void shouldPersistUnderstatExpectedGoalsAsCanonicalMatchStatistics() {
    when(seasons.findById(20L)).thenReturn(Optional.of(season));
    when(season.getLeague()).thenReturn(league);
    when(league.getCountry()).thenReturn(country);
    when(country.getId()).thenReturn(1L);
    when(teams.findAll()).thenReturn(List.of());
    when(teams.save(any(Team.class))).thenReturn(home, away);
    when(home.getId()).thenReturn(101L);
    when(away.getId()).thenReturn(102L);
    when(identities.findCanonicalId(ResourceType.TEAM, Provider.UNDERSTAT, "10"))
        .thenReturn(Optional.empty());
    when(identities.findCanonicalId(ResourceType.TEAM, Provider.UNDERSTAT, "20"))
        .thenReturn(Optional.empty());
    when(identities.findCanonicalId(ResourceType.MATCH, Provider.UNDERSTAT, "30"))
        .thenReturn(Optional.empty());
    when(matches.save(any(Match.class))).thenReturn(savedMatch);
    when(savedMatch.getId()).thenReturn(300L);

    var service =
        new ProviderSeasonPersistenceService(seasons, teams, matches, identities, statistics);
    service.persist(
        Provider.UNDERSTAT,
        20L,
        List.of(
            new ProviderMatchRecord(
                "30",
                "10",
                "Home Club",
                "20",
                "Away Club",
                Instant.parse("2024-08-16T19:00:00Z"),
                "FINISHED",
                2,
                1,
                null,
                new BigDecimal("1.42"),
                new BigDecimal("0.87"))));

    verify(statistics)
        .replaceFromProvider(
            org.mockito.ArgumentMatchers.eq(300L),
            org.mockito.ArgumentMatchers.eq(Provider.UNDERSTAT),
            org.mockito.ArgumentMatchers.argThat(
                values ->
                    values.size() == 1
                        && values.get(0).statisticKey().equals("expectedGoals")
                        && values.get(0).homeValue().compareTo(new BigDecimal("1.42")) == 0
                        && values.get(0).awayValue().compareTo(new BigDecimal("0.87")) == 0));
  }
  @Test
  void shouldLinkASecondProviderFixtureToTheExistingCanonicalMatch() {
    Instant kickoff = Instant.parse("2024-08-16T19:00:00Z");
    Match existingMatch = org.mockito.Mockito.mock(Match.class);
    when(seasons.findById(20L)).thenReturn(Optional.of(season));
    when(teams.findAll()).thenReturn(List.of(home, away));
    when(home.getId()).thenReturn(101L);
    when(away.getId()).thenReturn(102L);
    when(home.getName()).thenReturn("Home Club");
    when(away.getName()).thenReturn("Away Club");
    when(identities.findCanonicalId(ResourceType.TEAM, Provider.FOOTBALL_DATA, "home-1"))
        .thenReturn(Optional.empty());
    when(identities.findCanonicalId(ResourceType.TEAM, Provider.FOOTBALL_DATA, "away-2"))
        .thenReturn(Optional.empty());
    when(identities.findCanonicalId(ResourceType.MATCH, Provider.FOOTBALL_DATA, "match-3"))
        .thenReturn(Optional.empty());
    when(matches.findAllBySeasonIdOrderByKickoffAtAsc(20L))
        .thenReturn(List.of(existingMatch));
    when(existingMatch.getHomeTeam()).thenReturn(home);
    when(existingMatch.getAwayTeam()).thenReturn(away);
    when(existingMatch.getKickoffAt()).thenReturn(kickoff);
    when(existingMatch.getId()).thenReturn(300L);
    when(existingMatch.getStatus()).thenReturn(com.h2football.module.match.domain.model.MatchStatus.FINISHED);
    when(existingMatch.getHomeScore()).thenReturn(1);
    when(existingMatch.getAwayScore()).thenReturn(0);

    var service =
        new ProviderSeasonPersistenceService(seasons, teams, matches, identities, statistics);
    var result =
        service.persist(
            Provider.FOOTBALL_DATA,
            20L,
            List.of(
                new ProviderMatchRecord(
                    "match-3",
                    "home-1",
                    "Home Club",
                    "away-2",
                    "Away Club",
                    kickoff,
                    "FINISHED",
                    2,
                    1,
                    "1",
                    null,
                    null)));

    assertThat(result.created()).isZero();
    assertThat(result.linkedToExistingMatches()).isEqualTo(1);
    verify(identities).register(ResourceType.MATCH, Provider.FOOTBALL_DATA, "match-3", 300L);
    verify(existingMatch)
        .updateSnapshot(kickoff, com.h2football.module.match.domain.model.MatchStatus.FINISHED, null, null, "1");
    verify(matches, never()).save(any(Match.class));
  }

}
