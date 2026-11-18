package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(collectionResourceRel = "teams", path = "teams")
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByCountryIdAndNameIgnoreCase(Long countryId, String name);
    Optional<Team> findByCountryIdAndNameIgnoreCase(Long countryId, String name);

    @Override
    @EntityGraph(attributePaths = "country")
    List<Team> findAll();

    @Override
    @EntityGraph(attributePaths = "country")
    List<Team> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "country")
    Page<Team> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "country")
    Optional<Team> findById(Long id);

    @Override @RestResource(exported = false)
    <S extends Team> S save(S entity);
    @Override @RestResource(exported = false)
    <S extends Team> List<S> saveAll(Iterable<S> entities);
    @Override @RestResource(exported = false)
    void deleteById(Long id);
    @Override @RestResource(exported = false)
    void delete(Team entity);
    @Override @RestResource(exported = false)
    void deleteAllById(Iterable<? extends Long> ids);
    @Override @RestResource(exported = false)
    void deleteAll(Iterable<? extends Team> entities);
    @Override @RestResource(exported = false)
    void deleteAll();
}
