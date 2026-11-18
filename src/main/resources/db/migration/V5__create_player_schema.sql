CREATE TABLE players (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    short_name VARCHAR(80) NULL,
    slug VARCHAR(120) NULL,
    position VARCHAR(10) NULL,
    country_code VARCHAR(3) NULL,
    country_name VARCHAR(100) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_players_name (name),
    INDEX idx_players_country_code (country_code)
) ENGINE=InnoDB;
