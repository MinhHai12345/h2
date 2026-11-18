package com.h2football.module.match.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.h2football.module.identity.application.ExternalIdentityService;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import com.h2football.module.match.domain.repository.MatchRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchApplicationServiceTest {
  private final MatchRepository matches = mock(MatchRepository.class);
  private final SeasonRepository seasons = mock(SeasonRepository.class);
  private final TeamRepository teams = mock(TeamRepository.class);
  private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
  private final MatchApplicationService service =
      new MatchApplicationService(matches, seasons, teams, identities);

  @Test
  void updatesExistingCanonicalMatchInsteadOfCreatingDuplicate() {
    Match existing = mock(Match.class);
    Season season = mock(Season.class);
    Team home = mock(Team.class);
    Team away = mock(Team.class);
    Instant kickoff = Instant.parse("2026-11-18T10:00:00Z");
    when(identities.findCanonicalId(ResourceType.MATCH, Provider.SOFASCORE, "event-1"))
        .thenReturn(Optional.of(77L));
    when(matches.findById(77L)).thenReturn(Optional.of(existing));
    when(existing.getSeason()).thenReturn(season);
    when(existing.getHomeTeam()).thenReturn(home);
    when(existing.getAwayTeam()).thenReturn(away);
    when(season.getId()).thenReturn(3L);
    when(home.getId()).thenReturn(10L);
    when(away.getId()).thenReturn(20L);

    boolean created =
        service.upsertFromProvider(
            Provider.SOFASCORE,
            "event-1",
            3L,
            10L,
            20L,
            kickoff,
            MatchStatus.FINISHED,
            2,
            0,
            "Round 1");

    assertThat(created).isFalse();
    verify(existing).updateSnapshot(kickoff, MatchStatus.FINISHED, 2, 0, "Round 1");
    verify(matches, never()).save(any(Match.class));
  }

  @Test
  void createsMatchAndRegistersSharedIdentityWhenSourceEventIsNew() {
    Season season = mock(Season.class);
    Team home = mock(Team.class);
    Team away = mock(Team.class);
    Match saved = mock(Match.class);
    when(identities.findCanonicalId(ResourceType.MATCH, Provider.SOFASCORE, "event-2"))
        .thenReturn(Optional.empty());
    when(seasons.findById(3L)).thenReturn(Optional.of(season));
    when(teams.findById(10L)).thenReturn(Optional.of(home));
    when(teams.findById(20L)).thenReturn(Optional.of(away));
    when(matches.save(any(Match.class))).thenReturn(saved);
    when(saved.getId()).thenReturn(88L);

    boolean created =
        service.upsertFromProvider(
            Provider.SOFASCORE,
            "event-2",
            3L,
            10L,
            20L,
            Instant.parse("2026-11-18T10:00:00Z"),
            MatchStatus.NOT_STARTED,
            null,
            null,
            "Round 2");

    assertThat(created).isTrue();
    verify(matches).save(any(Match.class));
    verify(identities).register(ResourceType.MATCH, Provider.SOFASCORE, "event-2", 88L);
  }
}
