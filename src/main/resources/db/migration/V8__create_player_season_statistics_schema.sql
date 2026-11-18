CREATE TABLE player_season_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    season_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    appearances BIGINT NOT NULL DEFAULT 0,
    minutes_played BIGINT NOT NULL DEFAULT 0,
    goals BIGINT NOT NULL DEFAULT 0,
    assists BIGINT NOT NULL DEFAULT 0,
    total_shots BIGINT NOT NULL DEFAULT 0,
    shots_on_target BIGINT NOT NULL DEFAULT 0,
    accurate_passes BIGINT NOT NULL DEFAULT 0,
    total_passes BIGINT NOT NULL DEFAULT 0,
    key_passes BIGINT NOT NULL DEFAULT 0,
    tackles BIGINT NOT NULL DEFAULT 0,
    interceptions BIGINT NOT NULL DEFAULT 0,
    clearances BIGINT NOT NULL DEFAULT 0,
    expected_goals DECIMAL(14, 4) NULL,
    expected_assists DECIMAL(14, 4) NULL,
    average_rating DECIMAL(8, 3) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_player_season_statistics_season
        FOREIGN KEY (season_id) REFERENCES seasons (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_season_statistics_player
        FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT uk_player_season_statistics_source
        UNIQUE (season_id, player_id, provider),
    INDEX idx_player_season_statistics_season_provider (season_id, provider),
    INDEX idx_player_season_statistics_player (player_id)
) ENGINE=InnoDB;
