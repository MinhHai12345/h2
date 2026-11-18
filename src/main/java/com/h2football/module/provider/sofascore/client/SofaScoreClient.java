package com.h2football.module.provider.sofascore.client;

import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import com.h2football.module.provider.sofascore.dto.SofaScoreLeagueCatalogue;
import java.util.List;

public interface SofaScoreClient {
    SofaScoreLeagueCatalogue fetchLeagueCatalogue(List<Long> selectedTournamentIds);
    List<SofaScoreDtos.StandingGroup> fetchStandings(long tournamentId, long seasonId);
}
