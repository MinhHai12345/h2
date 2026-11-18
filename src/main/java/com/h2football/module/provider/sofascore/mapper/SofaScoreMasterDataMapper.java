package com.h2football.module.provider.sofascore.mapper;

import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import com.h2football.module.provider.sofascore.model.SofaScoreLeagueSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreSeasonSnapshot;
import com.h2football.module.provider.sofascore.model.SofaScoreTeamSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreMasterDataMapper {
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(20\\d{2}|\\d{2})(?!\\d)");

    public SofaScoreLeagueSnapshot toLeagueSnapshot(
            SofaScoreDtos.Tournament tournament, List<SofaScoreDtos.Season> sourceSeasons) {
        if (tournament == null || tournament.id() == null
                || tournament.name() == null || tournament.name().isBlank()) return null;
        String isoCode = tournament.category() == null ? null : normalizeIsoCode(tournament.category().alpha2());
        String countryName = tournament.category() == null ? null : clean(tournament.category().name());
        List<SofaScoreSeasonSnapshot> seasons = new ArrayList<>();
        if (sourceSeasons != null) {
            for (SofaScoreDtos.Season season : sourceSeasons) {
                SofaScoreSeasonSnapshot mapped = toSeasonSnapshot(season);
                if (mapped != null) seasons.add(mapped);
            }
        }
        return new SofaScoreLeagueSnapshot(
                tournament.id().toString(), clean(tournament.name()), isoCode, countryName, seasons);
    }

    public SofaScoreSeasonSnapshot toSeasonSnapshot(SofaScoreDtos.Season season) {
        if (season == null || season.id() == null) return null;
        String year = canonicalStartYear(season.year());
        if (year == null) year = canonicalStartYear(season.name());
        if (year == null) return null;
        return new SofaScoreSeasonSnapshot(season.id().toString(), year,
                season.name() == null || season.name().isBlank() ? year : clean(season.name()), season.current());
    }

    public SofaScoreTeamSnapshot toTeamSnapshot(SofaScoreDtos.Team team) {
        if (team == null || team.id() == null || team.name() == null || team.name().isBlank()) return null;
        String isoCode = team.country() == null ? null : normalizeIsoCode(team.country().alpha2());
        String countryName = team.country() == null ? null : clean(team.country().name());
        return new SofaScoreTeamSnapshot(team.id().toString(), clean(team.name()), isoCode, countryName);
    }

    public static String canonicalStartYear(String label) {
        if (label == null || label.isBlank()) return null;
        Matcher matcher = YEAR.matcher(label);
        if (!matcher.find()) return null;
        String year = matcher.group(1);
        if (year.length() == 4) return year;
        int shortYear = Integer.parseInt(year);
        return String.valueOf(shortYear <= 79 ? 2000 + shortYear : 1900 + shortYear);
    }

    private static String normalizeIsoCode(String value) {
        return value != null && value.matches("(?i)[a-z]{2}") ? value.toUpperCase(Locale.ROOT) : null;
    }

    private static String clean(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
