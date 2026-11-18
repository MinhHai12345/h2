CREATE TABLE match_analytics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    home_expected_goals DECIMAL(12, 6) NULL,
    away_expected_goals DECIMAL(12, 6) NULL,
    home_form_index DECIMAL(10, 6) NULL,
    away_form_index DECIMAL(10, 6) NULL,
    home_win_probability DECIMAL(10, 8) NULL,
    draw_probability DECIMAL(10, 8) NULL,
    away_win_probability DECIMAL(10, 8) NULL,
    over_2_5_probability DECIMAL(10, 8) NULL,
    both_teams_score_probability DECIMAL(10, 8) NULL,
    match_attractiveness_index DECIMAL(8, 3) NULL,
    home_history_matches INT NOT NULL DEFAULT 0,
    away_history_matches INT NOT NULL DEFAULT 0,
    model_version VARCHAR(40) NOT NULL,
    insufficient_data_reason VARCHAR(500) NULL,
    computed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_match_analytics_match
        FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    CONSTRAINT ck_match_analytics_history_counts
        CHECK (home_history_matches >= 0 AND away_history_matches >= 0),
    CONSTRAINT ck_match_analytics_mai
        CHECK (match_attractiveness_index IS NULL OR
               (match_attractiveness_index >= 0 AND match_attractiveness_index <= 100)),
    CONSTRAINT uk_match_analytics_source UNIQUE (match_id, provider),
    INDEX idx_match_analytics_provider_status (provider, status),
    INDEX idx_match_analytics_computed_at (computed_at)
) ENGINE=InnoDB;
