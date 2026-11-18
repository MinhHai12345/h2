package com.h2football.module.provider.sofascore.model;

public record SofaScorePlayerSnapshot(
    String externalId,
    String name,
    String shortName,
    String slug,
    String position,
    String countryCode,
    String countryName) {}
