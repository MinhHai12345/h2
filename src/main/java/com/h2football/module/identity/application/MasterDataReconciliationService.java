package com.h2football.module.identity.application;

import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.masterdata.domain.model.League;
import com.h2football.module.masterdata.domain.model.Season;
import com.h2football.module.masterdata.domain.model.Team;
import com.h2football.module.masterdata.domain.repository.LeagueRepository;
import com.h2football.module.masterdata.domain.repository.SeasonRepository;
import com.h2football.module.masterdata.domain.repository.TeamRepository;
import com.h2football.module.player.domain.model.Player;
import com.h2football.module.player.domain.repository.PlayerRepository;
import com.h2football.module.provider.standardization.ProviderDataStandardizer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MasterDataReconciliationService {
  private final LeagueRepository leagues;
  private final SeasonRepository seasons;
  private final TeamRepository teams;
  private final PlayerRepository players;

  public MasterDataReconciliationService(
      LeagueRepository leagues,
      SeasonRepository seasons,
      TeamRepository teams,
      PlayerRepository players) {
    this.leagues = leagues;
    this.seasons = seasons;
    this.teams = teams;
    this.players = players;
  }

  public List<ReconciliationCandidate> findCandidates(ResourceType resourceType) {
    if (resourceType != null && !supported(resourceType)) {
      throw new IllegalArgumentException(
          "Reconciliation supports LEAGUE, SEASON, TEAM and PLAYER");
    }
    List<ReconciliationCandidate> result = new ArrayList<>();
    if (resourceType == null || resourceType == ResourceType.LEAGUE) result.addAll(leagueCandidates());
    if (resourceType == null || resourceType == ResourceType.SEASON) result.addAll(seasonCandidates());
    if (resourceType == null || resourceType == ResourceType.TEAM) result.addAll(teamCandidates());
    if (resourceType == null || resourceType == ResourceType.PLAYER) result.addAll(playerCandidates());
    return result.stream()
        .sorted(Comparator.comparing(item -> item.resourceType().name() + item.matchKey()))
        .toList();
  }

  private List<ReconciliationCandidate> leagueCandidates() {
    Map<String, List<EntityReference>> groups = new TreeMap<>();
    for (League item : leagues.findAll()) {
      String key =
          item.getCountry().getId() + "|"
              + ProviderDataStandardizer.leagueMatchKey(item.getName());
      add(groups, key, new EntityReference(item.getId(), item.getName(), item.getCountry().getName()));
    }
    return candidates(ResourceType.LEAGUE, groups, "Same country and canonicalized league name");
  }

  private List<ReconciliationCandidate> seasonCandidates() {
    Map<String, List<EntityReference>> groups = new TreeMap<>();
    for (Season item : seasons.findAll()) {
      String key = item.getLeague().getId() + "|" + normalizeSeasonCode(item.getSeasonCode());
      add(
          groups,
          key,
          new EntityReference(
              item.getId(),
              item.getDisplayName() + " (" + item.getSeasonCode() + ")",
              item.getLeague().getName()));
    }
    return candidates(ResourceType.SEASON, groups, "Same league and normalized season code");
  }

  private List<ReconciliationCandidate> teamCandidates() {
    Map<String, List<EntityReference>> groups = new TreeMap<>();
    for (Team item : teams.findAll()) {
      String key =
          item.getCountry().getId() + "|"
              + ProviderDataStandardizer.teamMatchKey(item.getName());
      add(groups, key, new EntityReference(item.getId(), item.getName(), item.getCountry().getName()));
    }
    return candidates(ResourceType.TEAM, groups, "Same country and canonicalized team name");
  }

  private List<ReconciliationCandidate> playerCandidates() {
    Map<String, List<EntityReference>> groups = new TreeMap<>();
    for (Player item : players.findAll()) {
      if (item.getCountryCode() == null || item.getCountryCode().isBlank()) continue;
      String key =
          item.getCountryCode().trim().toUpperCase(Locale.ROOT)
              + "|" + ProviderDataStandardizer.playerMatchKey(item.getName());
      add(
          groups,
          key,
          new EntityReference(
              item.getId(), item.getName(), item.getCountryCode().trim().toUpperCase(Locale.ROOT)));
    }
    return candidates(
        ResourceType.PLAYER,
        groups,
        "Same normalized player name and country code; verify player identity before linking");
  }

  private List<ReconciliationCandidate> candidates(
      ResourceType type, Map<String, List<EntityReference>> groups, String reason) {
    return groups.entrySet().stream()
        .filter(entry -> entry.getValue().size() > 1)
        .map(
            entry ->
                new ReconciliationCandidate(
                    type,
                    entry.getKey(),
                    entry.getValue().stream().map(EntityReference::canonicalId).sorted().toList(),
                    List.copyOf(entry.getValue()),
                    reason,
                    "REVIEW_REQUIRED"))
        .toList();
  }

  private void add(Map<String, List<EntityReference>> groups, String key, EntityReference value) {
    if (key.endsWith("|")) return;
    groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
  }

  private String normalizeSeasonCode(String value) {
    return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "");
  }

  private boolean supported(ResourceType type) {
    return type == ResourceType.LEAGUE
        || type == ResourceType.SEASON
        || type == ResourceType.TEAM
        || type == ResourceType.PLAYER;
  }

  public record EntityReference(Long canonicalId, String name, String context) {}

  public record ReconciliationCandidate(
      ResourceType resourceType,
      String matchKey,
      List<Long> canonicalIds,
      List<EntityReference> entities,
      String reason,
      String status) {}
}
