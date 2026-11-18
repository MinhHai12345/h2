package com.h2football.module.provider.sofascore.model;

public record SofaScoreIncidentSnapshot(
    String externalId,
    int sortOrder,
    String incidentType,
    String incidentClass,
    Integer minute,
    Integer addedTime,
    Boolean isHome,
    String text,
    String reason,
    Integer homeScore,
    Integer awayScore,
    boolean rescinded,
    SofaScorePlayerSnapshot player,
    SofaScorePlayerSnapshot assistPlayer,
    SofaScorePlayerSnapshot playerIn,
    SofaScorePlayerSnapshot playerOut) {}
