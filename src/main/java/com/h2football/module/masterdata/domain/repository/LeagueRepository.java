package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.League;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(collectionResourceRel = "leagues", path = "leagues")
public interface LeagueRepository extends JpaRepository<League, Long> {

    boolean existsByCode(String code);

    @Override
    @EntityGraph(attributePaths = "country")
    List<League> findAll();

    @Override
    @EntityGraph(attributePaths = "country")
    List<League> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "country")
    Page<League> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "country")
    Optional<League> findById(Long id);

    @Override
    @RestResource(exported = false)
    <S extends League> S save(S entity);

    @Override
    @RestResource(exported = false)
    <S extends League> List<S> saveAll(Iterable<S> entities);

    @Override
    @RestResource(exported = false)
    void deleteById(Long id);

    @Override
    @RestResource(exported = false)
    void delete(League entity);

    @Override
    @RestResource(exported = false)
    void deleteAllById(Iterable<? extends Long> ids);

    @Override
    @RestResource(exported = false)
    void deleteAll(Iterable<? extends League> entities);

    @Override
    @RestResource(exported = false)
    void deleteAll();
}
