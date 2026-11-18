package com.h2football.module.provider.sofascore.model;

public record SofaScoreSeasonSnapshot(
    String externalId, String seasonCode, String displayName, boolean current) {}
