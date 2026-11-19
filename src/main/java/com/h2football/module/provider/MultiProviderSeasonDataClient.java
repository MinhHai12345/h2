package com.h2football.module.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.provider.footballdata.FootballDataClient;
import com.h2football.module.provider.footballdatauk.FootballDataUkClient;
import com.h2football.module.provider.understat.UnderstatClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MultiProviderSeasonDataClient {
  private static final DateTimeFormatter UK_DATE = DateTimeFormatter.ofPattern("dd/MM/yy");
  private static final DateTimeFormatter UNDERSTAT_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");

  private final UnderstatClient understatClient;
  private final FootballDataClient footballDataClient;
  private final FootballDataUkClient footballDataUkClient;

  public MultiProviderSeasonDataClient(
      UnderstatClient understatClient,
      FootballDataClient footballDataClient,
      FootballDataUkClient footballDataUkClient) {
    this.understatClient = understatClient;
    this.footballDataClient = footballDataClient;
    this.footballDataUkClient = footballDataUkClient;
  }

  public List<ProviderMatchRecord> fetch(
      Provider provider, String externalLeagueId, int seasonYear) {
    return switch (provider) {
      case UNDERSTAT -> fetchUnderstat(externalLeagueId, seasonYear);
      case FOOTBALL_DATA -> fetchFootballData(externalLeagueId, seasonYear);
      case FOOTBALL_DATA_UK -> fetchFootballDataUk(externalLeagueId, seasonYear);
      case SOFASCORE ->
          throw new IllegalArgumentException("SofaScore uses its dedicated ingestion pipeline");
    };
  }

  private List<ProviderMatchRecord> fetchFootballData(
      String competition, int seasonYear) {
    JsonNode root = footballDataClient.fetchCompetitionMatches(competition, seasonYear);
    List<ProviderMatchRecord> records = new ArrayList<>();
    JsonNode matches = root == null ? null : root.path("matches");
    if (matches != null && matches.isArray()) {
      for (JsonNode match : matches) {
        JsonNode home = match.path("homeTeam");
        JsonNode away = match.path("awayTeam");
        JsonNode score = match.path("score").path("fullTime");
        records.add(new ProviderMatchRecord(
            text(match, "id"), text(home, "id"), text(home, "name"),
            text(away, "id"), text(away, "name"), instant(text(match, "utcDate"), null),
            status(text(match, "status")), integer(score, "home"), integer(score, "away"),
            text(match, "matchday"), null, null));
      }
    }
    return List.copyOf(records);
  }

  private List<ProviderMatchRecord> fetchUnderstat(String league, int seasonYear) {
    JsonNode root = understatClient.fetchLeagueData(league, seasonYear);
    List<ProviderMatchRecord> records = new ArrayList<>();
    JsonNode dates = root == null ? null : root.path("dates");
    if (dates != null && dates.isArray()) {
      for (JsonNode match : dates) {
        JsonNode goals = match.path("goals");
        JsonNode xg = match.path("xG");
        Integer homeScore = nullableInteger(goals, "h");
        Integer awayScore = nullableInteger(goals, "a");
        boolean finished = match.path("isResult").asBoolean(false)
            || (homeScore != null && awayScore != null);
        records.add(new ProviderMatchRecord(
            text(match, "id"), text(match, "h"), text(match, "home_team"),
            text(match, "a"), text(match, "away_team"),
            instant(text(match, "datetime"), UNDERSTAT_DATE),
            finished ? "FINISHED" : "SCHEDULED", homeScore, awayScore,
            text(match, "round"), decimal(xg, "h"), decimal(xg, "a")));
      }
    }
    return List.copyOf(records);
  }

  private List<ProviderMatchRecord> fetchFootballDataUk(String leagueCode, int seasonYear) {
    String seasonCode = String.format(Locale.ROOT, "%02d%02d", seasonYear % 100, (seasonYear + 1) % 100);
    String csv = footballDataUkClient.fetchSeasonCsv(leagueCode, seasonYear);
    if (!StringUtils.hasText(csv)) return List.of();
    List<List<String>> rows = parseCsv(csv);
    if (rows.isEmpty()) return List.of();
    List<String> header =
        rows.get(0).stream()
            .map(
                value ->
                    value
                        .replace(String.valueOf((char) 0xFEFF), "")
                        .trim()
                        .toUpperCase(Locale.ROOT))
            .toList();
    List<ProviderMatchRecord> records = new ArrayList<>();
    for (int index = 1; index < rows.size(); index++) {
      List<String> row = rows.get(index);
      String date = cell(header, row, "DATE");
      String time = cell(header, row, "TIME");
      String home = cell(header, row, "HOMETEAM");
      String away = cell(header, row, "AWAYTEAM");
      if (!StringUtils.hasText(date) || !StringUtils.hasText(home) || !StringUtils.hasText(away)) {
        continue;
      }
      LocalDate localDate;
      try {
        localDate = LocalDate.parse(date, UK_DATE);
      } catch (RuntimeException exception) {
        try {
          localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (RuntimeException ignored) {
          continue;
        }
      }
      LocalTime localTime;
      try {
        localTime = LocalTime.parse(StringUtils.hasText(time) ? time : "15:00");
      } catch (RuntimeException exception) {
        localTime = LocalTime.NOON;
      }
      Instant kickoff = localDate.atTime(localTime).atZone(UK_ZONE).toInstant();
      String externalId =
          seasonCode + ":" + localDate + ":" + normalize(home) + ":" + normalize(away);
      Integer homeScore = parseInteger(cell(header, row, "FTHG"));
      Integer awayScore = parseInteger(cell(header, row, "FTAG"));
      records.add(
          new ProviderMatchRecord(
              externalId,
              normalize(home),
              home,
              normalize(away),
              away,
              kickoff,
              homeScore != null && awayScore != null ? "FINISHED" : "SCHEDULED",
              homeScore,
              awayScore,
              cell(header, row, "ROUND"),
              decimal(cell(header, row, "HXG")),
              decimal(cell(header, row, "AXG"))));
    }
    return List.copyOf(records);
  }

  private static List<List<String>> parseCsv(String csv) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < csv.length(); i++) {
      char current = csv.charAt(i);
      if (current == '"') {
        if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
          field.append('"');
          i++;
        } else {
          quoted = !quoted;
        }
      } else if (current == ',' && !quoted) {
        row.add(field.toString().trim());
        field.setLength(0);
      } else if ((current == '\n' || current == '\r') && !quoted) {
        if (current == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
        row.add(field.toString().trim());
        field.setLength(0);
        if (row.stream().anyMatch(StringUtils::hasText)) rows.add(List.copyOf(row));
        row.clear();
      } else {
        field.append(current);
      }
    }
    row.add(field.toString().trim());
    if (row.stream().anyMatch(StringUtils::hasText)) rows.add(List.copyOf(row));
    return rows;
  }

  private static String cell(List<String> header, List<String> row, String name) {
    int index = header.indexOf(name);
    return index < 0 || index >= row.size() ? "" : row.get(index).trim();
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private static Integer integer(JsonNode node, String field) {
    return nullableInteger(node, field);
  }

  private static Integer nullableInteger(JsonNode node, String field) {
    String value = text(node, field);
    return parseInteger(value);
  }

  private static BigDecimal decimal(JsonNode node, String field) {
    return decimal(text(node, field));
  }

  private static BigDecimal decimal(String value) {
    if (!StringUtils.hasText(value) || "-".equals(value)) return null;
    try {
      BigDecimal result = new BigDecimal(value.trim());
      return result.signum() >= 0 ? result : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static Integer parseInteger(String value) {
    if (!StringUtils.hasText(value) || "-".equals(value)) return null;
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static Instant instant(String value, DateTimeFormatter formatter) {
    if (!StringUtils.hasText(value)) return Instant.EPOCH;
    try {
      if (formatter == null) return Instant.parse(value);
      return LocalDateTime.parse(value, formatter).toInstant(ZoneOffset.UTC);
    } catch (RuntimeException exception) {
      return Instant.EPOCH;
    }
  }

  private static String status(String value) {
    return value == null ? "UNKNOWN" : value.toUpperCase(Locale.ROOT);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  public record ProviderMatchRecord(
      String externalMatchId,
      String homeTeamExternalId,
      String homeTeamName,
      String awayTeamExternalId,
      String awayTeamName,
      Instant kickoffAt,
      String status,
      Integer homeScore,
      Integer awayScore,
      String roundName,
      BigDecimal homeExpectedGoals,
      BigDecimal awayExpectedGoals) {}
}
