package com.h2football.module.masterdata.application;

import com.h2football.module.masterdata.domain.model.League;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeasonApplicationServiceTest {
    @Test
    void shouldRejectEndDateBeforeStartDate() {
        SeasonRepository seasons = mock(SeasonRepository.class);
        LeagueRepository leagues = mock(LeagueRepository.class);
        SeasonApplicationService service = new SeasonApplicationService(seasons, leagues);

        assertThatThrownBy(() -> service.create(1L, "2025", "2025", LocalDate.parse("2025-08-01"), LocalDate.parse("2025-07-01")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(seasons, leagues);
    }

    @Test
    void shouldCreateSeasonForExistingLeague() {
        SeasonRepository seasons = mock(SeasonRepository.class);
        LeagueRepository leagues = mock(LeagueRepository.class);
        League league = mock(League.class);
        when(seasons.existsByLeagueIdAndSeasonCode(1L, "2025")).thenReturn(false);
        when(leagues.findById(1L)).thenReturn(Optional.of(league));
        when(seasons.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SeasonApplicationService service = new SeasonApplicationService(seasons, leagues);

        Season created = service.create(1L, "2025", "2025/2026", LocalDate.parse("2025-08-01"), LocalDate.parse("2026-05-31"));

        assertThat(created.getSeasonCode()).isEqualTo("2025");
        assertThat(created.getDisplayName()).isEqualTo("2025/2026");
        verify(seasons).save(any(Season.class));
    }
}