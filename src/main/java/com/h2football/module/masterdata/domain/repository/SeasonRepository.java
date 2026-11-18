package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Season;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(collectionResourceRel = "seasons", path = "seasons")
public interface SeasonRepository extends JpaRepository<Season, Long> {

  boolean existsByLeagueIdAndSeasonCode(Long leagueId, String seasonCode);

  @EntityGraph(attributePaths = "league")
  List<Season> findAllByLeagueIdOrderByStartDateDesc(Long leagueId);

  @Override
  @EntityGraph(attributePaths = "league")
  List<Season> findAll();

  @Override
  @EntityGraph(attributePaths = "league")
  Page<Season> findAll(Pageable pageable);

  @Override
  @EntityGraph(attributePaths = "league")
  Optional<Season> findById(Long id);

  @Override
  @RestResource(exported = false)
  <S extends Season> S save(S entity);

  @Override
  @RestResource(exported = false)
  <S extends Season> List<S> saveAll(Iterable<S> entities);

  @Override
  @RestResource(exported = false)
  void deleteById(Long id);

  @Override
  @RestResource(exported = false)
  void delete(Season entity);

  @Override
  @RestResource(exported = false)
  void deleteAllById(Iterable<? extends Long> ids);

  @Override
  @RestResource(exported = false)
  void deleteAll(Iterable<? extends Season> entities);

  @Override
  @RestResource(exported = false)
  void deleteAll();
}
