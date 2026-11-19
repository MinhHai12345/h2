CREATE TABLE match_prediction_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    model_version VARCHAR(40) NOT NULL,
    computed_at TIMESTAMP(6) NOT NULL,
    home_win_probability DECIMAL(10, 8) NOT NULL,
    draw_probability DECIMAL(10, 8) NOT NULL,
    away_win_probability DECIMAL(10, 8) NOT NULL,
    actual_home_score INT NULL,
    actual_away_score INT NULL,
    actual_outcome VARCHAR(10) NULL,
    brier_score DECIMAL(12, 10) NULL,
    log_loss DECIMAL(12, 10) NULL,
    evaluated_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_prediction_snapshots_match
        FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    CONSTRAINT ck_prediction_snapshot_probabilities
        CHECK (
            home_win_probability >= 0 AND home_win_probability <= 1
            AND draw_probability >= 0 AND draw_probability <= 1
            AND away_win_probability >= 0 AND away_win_probability <= 1
            AND ABS(home_win_probability + draw_probability + away_win_probability - 1) <= 0.000001
        ),
    CONSTRAINT ck_prediction_snapshot_scores
        CHECK (
            (actual_home_score IS NULL AND actual_away_score IS NULL AND actual_outcome IS NULL)
            OR
            (actual_home_score >= 0 AND actual_away_score >= 0 AND actual_outcome IN ('HOME', 'DRAW', 'AWAY'))
        ),
    CONSTRAINT uk_prediction_snapshot_version_time
        UNIQUE (match_id, provider, model_version, computed_at),
    INDEX idx_prediction_snapshot_match_provider (match_id, provider, computed_at),
    INDEX idx_prediction_snapshot_model_version (model_version, evaluated_at)
) ENGINE=InnoDB;
