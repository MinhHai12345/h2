CREATE TABLE team_season_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    season_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    period VARCHAR(20) NOT NULL,
    group_name VARCHAR(80) NOT NULL,
    statistic_key VARCHAR(100) NOT NULL,
    statistic_name VARCHAR(120) NOT NULL,
    matches_count BIGINT NOT NULL DEFAULT 0,
    total_value DECIMAL(18, 4) NULL,
    average_value DECIMAL(18, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_team_season_statistics_season
        FOREIGN KEY (season_id) REFERENCES seasons (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_season_statistics_team
        FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT ck_team_season_statistics_matches_count CHECK (matches_count >= 0),
    CONSTRAINT uk_team_season_statistics_source
        UNIQUE (season_id, team_id, provider, period, statistic_key),
    INDEX idx_team_season_statistics_season_provider
        (season_id, provider, team_id),
    INDEX idx_team_season_statistics_team
        (team_id, season_id)
) ENGINE=InnoDB;
