package com.h2football.module.provider.sofascore;

import com.h2football.module.provider.sofascore.client.SofaScoreClient;
import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import com.h2football.module.provider.sofascore.dto.SofaScoreLeagueCatalogue;
import com.h2football.module.provider.sofascore.mapper.SofaScoreMasterDataMapper;
import com.h2football.module.provider.sofascore.model.SofaScoreLeagueSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreTeamSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreMasterDataProvider {
    private final SofaScoreClient client;
    private final SofaScoreMasterDataMapper mapper;

    public SofaScoreMasterDataProvider(SofaScoreClient client, SofaScoreMasterDataMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public List<SofaScoreLeagueSnapshot> fetchLeagues(List<Long> selectedTournamentIds) {
        SofaScoreLeagueCatalogue catalogue = client.fetchLeagueCatalogue(selectedTournamentIds);
        return catalogue.tournaments().stream()
                .map(tournament -> mapper.toLeagueSnapshot(
                        tournament, catalogue.seasonsByTournamentId().getOrDefault(tournament.id(), List.of())))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<SofaScoreTeamSnapshot> fetchTeams(long tournamentId, long seasonId) {
        Map<String, SofaScoreTeamSnapshot> teams = new LinkedHashMap<>();
        List<SofaScoreDtos.StandingGroup> groups = client.fetchStandings(tournamentId, seasonId);
        for (SofaScoreDtos.StandingGroup group : groups) {
            if (group == null || group.rows() == null) continue;
            for (SofaScoreDtos.StandingRow row : group.rows()) {
                SofaScoreTeamSnapshot team = row == null ? null : mapper.toTeamSnapshot(row.team());
                if (team != null) teams.putIfAbsent(team.externalId(), team);
            }
        }
        return List.copyOf(teams.values());
    }
}
