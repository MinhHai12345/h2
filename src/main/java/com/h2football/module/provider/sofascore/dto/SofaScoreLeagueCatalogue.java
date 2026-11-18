package com.h2football.module.provider.sofascore.dto;

import java.util.List;
import java.util.Map;

public record SofaScoreLeagueCatalogue(
        List<SofaScoreDtos.Tournament> tournaments,
        Map<Long, List<SofaScoreDtos.Season>> seasonsByTournamentId) {
    public SofaScoreLeagueCatalogue {
        tournaments = tournaments == null ? List.of() : List.copyOf(tournaments);
        seasonsByTournamentId = seasonsByTournamentId == null ? Map.of()
                : seasonsByTournamentId.entrySet().stream().collect(
                        java.util.stream.Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue() == null ? List.of() : List.copyOf(entry.getValue())));
    }
}
