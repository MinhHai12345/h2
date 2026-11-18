package com.h2football.module.provider.sofascore.model;

import java.util.List;

public record SofaScoreLeagueSnapshot(String externalId, String name, String countryIsoCode,
        String countryName, List<SofaScoreSeasonSnapshot> seasons) {
    public SofaScoreLeagueSnapshot {
        seasons = seasons == null ? List.of() : List.copyOf(seasons);
    }
}
