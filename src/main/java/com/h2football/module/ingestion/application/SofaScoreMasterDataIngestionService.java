package com.h2football.module.ingestion.application;

import com.h2football.module.masterdata.application.MasterDataImportApplicationService;
import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.provider.sofascore.SofaScoreMasterDataProvider;
import com.h2football.module.provider.sofascore.model.SofaScoreLeagueSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreSeasonSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreTeamSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SofaScoreMasterDataIngestionService {
    private static final String PROVIDER = "SOFASCORE";
    private final SofaScoreMasterDataProvider provider;
    private final MasterDataImportApplicationService masterData;

    public SofaScoreMasterDataIngestionService(
            SofaScoreMasterDataProvider provider, MasterDataImportApplicationService masterData) {
        this.provider = provider;
        this.masterData = masterData;
    }

    public LeagueSyncResult syncLeagues(List<Long> selectedTournamentIds) {
        List<SofaScoreLeagueSnapshot> snapshots = provider.fetchLeagues(selectedTournamentIds);
        List<String> warnings = new ArrayList<>();
        List<DiscoveredLeague> discovered = new ArrayList<>();
        int imported = 0;
        int skipped = 0;
        int discoveredSeasons = 0;

        for (SofaScoreLeagueSnapshot snapshot : snapshots) {
            discoveredSeasons += snapshot.seasons().size();
            discovered.add(new DiscoveredLeague(
                    snapshot.externalId(), snapshot.name(), snapshot.countryIsoCode(),
                    snapshot.countryName(), snapshot.seasons().stream()
                            .map(season -> new DiscoveredSeason(
                                    season.externalId(), season.seasonCode(), season.displayName(), season.current()))
                            .toList()));
            if (snapshot.countryIsoCode() == null || snapshot.countryName() == null) {
                warnings.add("Skipped tournament " + snapshot.externalId()
                        + ": missing valid ISO-2 country metadata.");
                skipped++;
                continue;
            }
            Country country = masterData.importCountry(
                    PROVIDER, snapshot.countryIsoCode(), snapshot.countryIsoCode(), snapshot.countryName());
            masterData.importLeague(PROVIDER, snapshot.externalId(),
                    "SOFASCORE_" + snapshot.externalId(), snapshot.name(), country.getId());
            imported++;
        }

        if (discoveredSeasons > 0) {
            warnings.add("Season catalogue was discovered but not persisted: SofaScore season catalogue "
                    + "does not provide the competition startDate/endDate required by the canonical Season model.");
        }
        return new LeagueSyncResult(snapshots.size(), imported, skipped,
                discoveredSeasons, discoveredSeasons, List.copyOf(discovered), List.copyOf(warnings));
    }

    public TeamSyncResult syncTeams(long tournamentId, long seasonId) {
        if (tournamentId <= 0 || seasonId <= 0) {
            throw new IllegalArgumentException("tournamentId and seasonId must be positive");
        }
        List<SofaScoreTeamSnapshot> snapshots = provider.fetchTeams(tournamentId, seasonId);
        List<String> warnings = new ArrayList<>();
        int imported = 0;
        int skipped = 0;
        for (SofaScoreTeamSnapshot snapshot : snapshots) {
            if (snapshot.countryIsoCode() == null || snapshot.countryName() == null) {
                warnings.add("Skipped team " + snapshot.externalId() + ": missing valid ISO-2 country metadata.");
                skipped++;
                continue;
            }
            Country country = masterData.importCountry(
                    PROVIDER, snapshot.countryIsoCode(), snapshot.countryIsoCode(), snapshot.countryName());
            masterData.importTeam(PROVIDER, snapshot.externalId(), snapshot.name(), country.getId());
            imported++;
        }
        return new TeamSyncResult(snapshots.size(), imported, skipped, List.copyOf(warnings));
    }

    public record DiscoveredSeason(String externalId, String seasonCode, String displayName, boolean current) { }

    public record DiscoveredLeague(String externalId, String name, String countryIsoCode,
            String countryName, List<DiscoveredSeason> seasons) { }

    public record LeagueSyncResult(int discoveredLeagues, int importedLeagues, int skippedLeagues,
            int discoveredSeasons, int skippedSeasons, List<DiscoveredLeague> leagues, List<String> warnings) { }

    public record TeamSyncResult(int discoveredTeams, int importedTeams, int skippedTeams, List<String> warnings) { }
}
