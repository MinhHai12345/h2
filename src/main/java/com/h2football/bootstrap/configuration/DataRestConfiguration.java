package com.h2football.bootstrap.configuration;

import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.model.League;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.MediaTypes;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class DataRestConfiguration implements RepositoryRestConfigurer {

  @Override
  public void configureRepositoryRestConfiguration(
      RepositoryRestConfiguration config, CorsRegistry cors) {
    config.setBasePath("/api/data");
    config.setDefaultMediaType(MediaTypes.HAL_JSON);
    config.setRepositoryDetectionStrategy(RepositoryDetectionStrategies.ANNOTATED);
    config.exposeIdsFor(Country.class, League.class, Season.class, Team.class);
  }
}
