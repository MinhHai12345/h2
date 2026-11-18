package com.h2football.module.provider.sofascore.model;

import java.time.Instant;

public record SofaScoreEventSnapshot(
    String externalId,
    Instant kickoffAt,
    String homeTeamExternalId,
    String awayTeamExternalId,
    String statusCode,
    Integer homeScore,
    Integer awayScore,
    String roundName) {}
