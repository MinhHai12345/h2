package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Country;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(collectionResourceRel = "countries", path = "countries")
public interface CountryRepository extends JpaRepository<Country, Long> {

  boolean existsByIsoCode(String isoCode);

  Optional<Country> findByIsoCode(String isoCode);

  @Override
  @RestResource(exported = false)
  <S extends Country> S save(S entity);

  @Override
  @RestResource(exported = false)
  <S extends Country> List<S> saveAll(Iterable<S> entities);

  @Override
  @RestResource(exported = false)
  void deleteById(Long id);

  @Override
  @RestResource(exported = false)
  void delete(Country entity);

  @Override
  @RestResource(exported = false)
  void deleteAllById(Iterable<? extends Long> ids);

  @Override
  @RestResource(exported = false)
  void deleteAll(Iterable<? extends Country> entities);

  @Override
  @RestResource(exported = false)
  void deleteAll();
}
