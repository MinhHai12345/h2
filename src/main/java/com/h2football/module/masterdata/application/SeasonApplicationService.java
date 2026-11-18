package com.h2football.module.masterdata.application;

import com.h2football.module.masterdata.domain.model.League;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SeasonApplicationService {
  private final SeasonRepository seasons;
  private final LeagueRepository leagues;

  public SeasonApplicationService(SeasonRepository seasons, LeagueRepository leagues) {
    this.seasons = seasons;
    this.leagues = leagues;
  }

  public Season create(
      Long leagueId,
      String seasonCode,
      String displayName,
      LocalDate startDate,
      LocalDate endDate) {
    if (endDate.isBefore(startDate))
      throw new IllegalArgumentException("endDate must not be before startDate");
    String normalizedCode = seasonCode.trim();
    if (seasons.existsByLeagueIdAndSeasonCode(leagueId, normalizedCode))
      throw new DuplicateMasterDataException("Season already exists for league: " + normalizedCode);
    League league =
        leagues
            .findById(leagueId)
            .orElseThrow(() -> new MasterDataNotFoundException("League", leagueId));
    return seasons.save(new Season(league, normalizedCode, displayName.trim(), startDate, endDate));
  }

  @Transactional(readOnly = true)
  public List<Season> findAll(Long leagueId) {
    return leagueId == null
        ? seasons.findAll()
        : seasons.findAllByLeagueIdOrderByStartDateDesc(leagueId);
  }

  @Transactional(readOnly = true)
  public Season get(Long id) {
    return seasons.findById(id).orElseThrow(() -> new MasterDataNotFoundException("Season", id));
  }
}
