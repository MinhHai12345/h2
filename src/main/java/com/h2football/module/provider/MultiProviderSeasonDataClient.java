package com.h2football.module.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.h2football.module.identity.domain.model.Provider;
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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class MultiProviderSeasonDataClient {
  private static final DateTimeFormatter UK_DATE = DateTimeFormatter.ofPattern("dd/MM/yy");
  private static final DateTimeFormatter UNDERSTAT_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");

  private final WebClient.Builder webClientBuilder;
  private final ProviderIngestionProperties properties;
  private long nextFootballDataRequestAtMillis;

  public MultiProviderSeasonDataClient(
      WebClient.Builder webClientBuilder, ProviderIngestionProperties properties) {
    this.webClientBuilder = webClientBuilder;
    this.properties = properties;
  }

  public List<ProviderMatchRecord> fetch(
      Provider provider, String externalLeagueId, int seasonYear) {
    return switch (provider) {
      case UNDERSTAT -> fetchUnderstat(externalLeagueId, seasonYear);
      case FOOTBALL_DATA -> fetchFootballData(externalLeagueId, seasonYear);
      case FOOTBALL_DATA_UK -> fetchFootballDataUk(externalLeagueId, seasonYear);
      case SOFASCORE -> throw new IllegalArgumentException("SofaScore uses its dedicated ingestion pipeline");
    };
  }

  private synchronized List<ProviderMatchRecord> fetchFootballData(String competition, int seasonYear) {
    var config = properties.getFootballData();
    if (!config.isEnabled()) throw new IllegalStateException("Football-Data provider is disabled");
    if (!StringUtils.hasText(config.getApiKey())) {
      throw new IllegalStateException("FOOTBALL_DATA_API_KEY is required");
    }
    awaitFootballDataRequestSlot();
    JsonNode root;
    try {
      root =
          webClientBuilder
              .baseUrl(config.getBaseUrl())
              .defaultHeader("X-Auth-Token", config.getApiKey())
              .defaultHeader(HttpHeaders.ACCEPT, "application/json")
              .build()
              .get()
              .uri(
                  uri ->
                      uri.path("/competitions/{code}/matches")
                          .queryParam("season", seasonYear)
                          .build(competition))
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block();
    } catch (RuntimeException exception) {
      nextFootballDataRequestAtMillis = System.currentTimeMillis() + 6_000;
      throw exception;
    }
    nextFootballDataRequestAtMillis = System.currentTimeMillis() + 6_000;
    List<ProviderMatchRecord> records = new ArrayList<>();
    JsonNode matches = root == null ? null : root.path("matches");
    if (matches != null && matches.isArray()) {
      for (JsonNode match : matches) {
        JsonNode home = match.path("homeTeam");
        JsonNode away = match.path("awayTeam");
        JsonNode score = match.path("score").path("fullTime");
        records.add(
            new ProviderMatchRecord(
                text(match, "id"),
                text(home, "id"),
                text(home, "name"),
                text(away, "id"),
                text(away, "name"),
                instant(text(match, "utcDate"), null),
                status(text(match, "status")),
                integer(score, "home"),
                integer(score, "away"),
                text(match, "matchday"),
                null,
                null));
      }
    }
    return List.copyOf(records);
  }

  private void awaitFootballDataRequestSlot() {
    long waitMillis = nextFootballDataRequestAtMillis - System.currentTimeMillis();
    if (waitMillis <= 0) return;
    try {
      Thread.sleep(waitMillis);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for football-data.org request slot", exception);
    }
  }

  private List<ProviderMatchRecord> fetchUnderstat(String league, int seasonYear) {
    var config = properties.getUnderstat();
    if (!config.isEnabled()) throw new IllegalStateException("Understat provider is disabled");
    JsonNode root =
        webClientBuilder
            .baseUrl(config.getBaseUrl())
            .defaultHeader(HttpHeaders.REFERER, config.getBaseUrl() + "/league/" + league + "/" + seasonYear)
            .defaultHeader("X-Requested-With", "XMLHttpRequest")
            .build()
            .get()
            .uri("/getLeagueData/{league}/{season}", league, seasonYear)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    List<ProviderMatchRecord> records = new ArrayList<>();
    JsonNode dates = root == null ? null : root.path("dates");
    if (dates != null && dates.isArray()) {
      for (JsonNode match : dates) {
        JsonNode goals = match.path("goals");
        String matchId = text(match, "id");
        String homeId = text(match, "h");
        String awayId = text(match, "a");
        String homeName = text(match, "home_team");
        String awayName = text(match, "away_team");
        String date = text(match, "datetime");
        JsonNode xg = match.path("xG");
        BigDecimal homeXg = decimal(xg, "h");
        BigDecimal awayXg = decimal(xg, "a");
        Integer homeScore = nullableInteger(goals, "h");
        Integer awayScore = nullableInteger(goals, "a");
        boolean finished = match.path("isResult").asBoolean(false) || (homeScore != null && awayScore != null);
        records.add(
            new ProviderMatchRecord(
                matchId,
                homeId,
                homeName,
                awayId,
                awayName,
                instant(date, UNDERSTAT_DATE),
                finished ? "FINISHED" : "SCHEDULED",
                homeScore,
                awayScore,
                text(match, "round"),
                homeXg,
                awayXg));
      }
    }
    return List.copyOf(records);
  }

  private List<ProviderMatchRecord> fetchFootballDataUk(String leagueCode, int seasonYear) {
    var config = properties.getFootballDataUk();
    if (!config.isEnabled()) throw new IllegalStateException("Football-Data UK provider is disabled");
    String seasonCode =
        String.format(Locale.ROOT, "%02d%02d", seasonYear % 100, (seasonYear + 1) % 100);
    String csv =
        webClientBuilder
            .baseUrl(config.getBaseUrl())
            .build()
            .get()
            .uri("/mmz4281/{season}/{league}.csv", seasonCode, leagueCode.toUpperCase(Locale.ROOT))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    if (!StringUtils.hasText(csv)) return List.of();
    List<List<String>> rows = parseCsv(csv);
    if (rows.isEmpty()) return List.of();
    List<String> header =
        rows.get(0).stream()
            .map(value -> value.replace(String.valueOf((char) 0xFEFF), "").trim().toUpperCase(Locale.ROOT))
            .toList();
    List<ProviderMatchRecord> records = new ArrayList<>();
    for (int index = 1; index < rows.size(); index++) {
      List<String> row = rows.get(index);
      String date = cell(header, row, "DATE");
      String time = cell(header, row, "TIME");
      String home = cell(header, row, "HOMETEAM");
      String away = cell(header, row, "AWAYTEAM");
      if (!StringUtils.hasText(date) || !StringUtils.hasText(home) || !StringUtils.hasText(away)) continue;
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
