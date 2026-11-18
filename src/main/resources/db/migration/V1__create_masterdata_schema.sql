CREATE TABLE countries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    iso_code CHAR(2) NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_countries_iso_code UNIQUE (iso_code),
    CONSTRAINT uk_countries_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE leagues (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    country_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_leagues_code UNIQUE (code),
    CONSTRAINT fk_leagues_country FOREIGN KEY (country_id) REFERENCES countries (id)
) ENGINE=InnoDB;

CREATE TABLE seasons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    league_id BIGINT NOT NULL,
    season_code VARCHAR(30) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_seasons_league_code UNIQUE (league_id, season_code),
    CONSTRAINT ck_seasons_date_range CHECK (end_date >= start_date),
    CONSTRAINT fk_seasons_league FOREIGN KEY (league_id) REFERENCES leagues (id)
) ENGINE=InnoDB;

CREATE TABLE teams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    country_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_teams_country_name UNIQUE (country_id, name),
    CONSTRAINT fk_teams_country FOREIGN KEY (country_id) REFERENCES countries (id)
) ENGINE=InnoDB;

CREATE TABLE masterdata_external_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resource_type VARCHAR(30) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(120) NOT NULL,
    canonical_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_masterdata_external_identity UNIQUE (resource_type, provider, external_id),
    INDEX idx_masterdata_canonical_identity (resource_type, canonical_id)
) ENGINE=InnoDB;
