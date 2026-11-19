package com.h2football.module.match.domain.repository;

import com.h2football.module.match.domain.model.Match;
import com.h2football.module.match.domain.model.MatchStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
  @EntityGraph(attributePaths = {"season.league", "homeTeam", "awayTeam"})
  List<Match> findAllBySeasonIdOrderByKickoffAtAsc(Long seasonId);

  @EntityGraph(attributePaths = {"season.league", "homeTeam", "awayTeam"})
  List<Match> findAllByStatusAndKickoffAtBetweenOrderByKickoffAtAsc(
      MatchStatus status, Instant start, Instant end);

  @Override
  @EntityGraph(attributePaths = {"season.league", "homeTeam", "awayTeam"})
  Optional<Match> findById(Long id);
}
