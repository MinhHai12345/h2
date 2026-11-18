CREATE TABLE match_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    period VARCHAR(20) NOT NULL,
    group_name VARCHAR(80) NOT NULL,
    statistic_key VARCHAR(100) NOT NULL,
    statistic_name VARCHAR(120) NOT NULL,
    home_display VARCHAR(80) NULL,
    away_display VARCHAR(80) NULL,
    home_value DECIMAL(14, 4) NULL,
    away_value DECIMAL(14, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_match_statistics_match FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    CONSTRAINT uk_match_statistics_source UNIQUE (match_id, provider, period, statistic_key),
    INDEX idx_match_statistics_match_period (match_id, period)
) ENGINE=InnoDB;
