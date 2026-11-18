package com.h2football.module.provider.sofascore.model;

import java.math.BigDecimal;

public record SofaScoreStatisticSnapshot(
    String period,
    String groupName,
    String statisticKey,
    String statisticName,
    String homeDisplay,
    String awayDisplay,
    BigDecimal homeValue,
    BigDecimal awayValue) {}
