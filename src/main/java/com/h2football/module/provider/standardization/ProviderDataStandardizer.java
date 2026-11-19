package com.h2football.module.provider.standardization;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Provider-neutral naming rules for values persisted in H2 canonical master data. */
public final class ProviderDataStandardizer {
  private static final Map<String, String> LEAGUE_ALIASES =
      Map.ofEntries(
          Map.entry("laliga", "LaLiga"),
          Map.entry("primera division", "LaLiga"),
          Map.entry("primera división", "LaLiga"),
          Map.entry("spanish primera division", "LaLiga"),
          Map.entry("english premier league", "Premier League"),
          Map.entry("premierleague", "Premier League"),
          Map.entry("1 bundesliga", "Bundesliga"),
          Map.entry("bundesliga", "Bundesliga"),
          Map.entry("seriea", "Serie A"),
          Map.entry("ligue1", "Ligue 1"),
          Map.entry("championship", "EFL Championship"),
          Map.entry("english championship", "EFL Championship"),
          Map.entry("uefa champions league", "UEFA Champions League"),
          Map.entry("championsleague", "UEFA Champions League"),
          Map.entry("fifa world cup", "FIFA World Cup"));

  private static final Map<String, String> STATISTIC_ALIASES =
      Map.ofEntries(
          Map.entry("xg", "expectedGoals"),
          Map.entry("expectedgoals", "expectedGoals"),
          Map.entry("totalshots", "totalShots"),
          Map.entry("shots", "totalShots"),
          Map.entry("shotsontarget", "shotsOnTarget"),
          Map.entry("shotsinsidebox", "shotsInsideBox"),
          Map.entry("shotsofftarget", "shotsOffTarget"),
          Map.entry("possession", "possession"),
          Map.entry("bigchances", "bigChances"),
          Map.entry("corners", "corners"),
          Map.entry("fouls", "fouls"),
          Map.entry("yellowcards", "yellowCards"),
          Map.entry("redcards", "redCards"),
          Map.entry("accuratepasses", "accuratePasses"),
          Map.entry("passaccuracy", "passAccuracy"),
          Map.entry("saves", "saves"),
          Map.entry("offsides", "offsides"),
          Map.entry("tackles", "tackles"),
          Map.entry("interceptions", "interceptions"),
          Map.entry("clearances", "clearances"));

  private static final Map<String, String> TEAM_ALIASES =
      Map.ofEntries(
          Map.entry("fcbayernmunchen", "Bayern Munich"),
          Map.entry("bayernmunchen", "Bayern Munich"),
          Map.entry("bayernmunich", "Bayern Munich"),
          Map.entry("bayer04leverkusen", "Bayer Leverkusen"),
          Map.entry("bayerleverkusen", "Bayer Leverkusen"),
          Map.entry("1fckoln", "FC Köln"),
          Map.entry("fckoln", "FC Köln"),
          Map.entry("fccologne", "FC Köln"),
          Map.entry("1fcunionberlin", "Union Berlin"),
          Map.entry("unionberlin", "Union Berlin"),
          Map.entry("1fsvmainz05", "Mainz 05"),
          Map.entry("mainz05", "Mainz 05"),
          Map.entry("rasenballsportleipzig", "RB Leipzig"),
          Map.entry("rbleipzig", "RB Leipzig"),
          Map.entry("borussiamgladbach", "Borussia Mönchengladbach"),
          Map.entry("borussiamonchengladbach", "Borussia Mönchengladbach"),
          Map.entry("fcinternazionalemilano", "Inter"),
          Map.entry("intermilan", "Inter"),
          Map.entry("internazionale", "Inter"),
          Map.entry("acmilan", "AC Milan"),
          Map.entry("milan", "AC Milan"),
          Map.entry("juventusfc", "Juventus"),
          Map.entry("parissaintgermainfc", "Paris Saint-Germain"),
          Map.entry("psg", "Paris Saint-Germain"),
          Map.entry("parissaintgermain", "Paris Saint-Germain"),
          Map.entry("atleticodemadrid", "Atletico Madrid"),
          Map.entry("clubatleticodemadrid", "Atletico Madrid"),
          Map.entry("atleticomadrid", "Atletico Madrid"),
          Map.entry("fcbarcelona", "Barcelona"),
          Map.entry("barcelona", "Barcelona"),
          Map.entry("realmadridcf", "Real Madrid"),
          Map.entry("realmadrid", "Real Madrid"),
          Map.entry("manchesterunitedfc", "Manchester United"),
          Map.entry("manutd", "Manchester United"),
          Map.entry("manchesterunited", "Manchester United"),
          Map.entry("manchestercityfc", "Manchester City"),
          Map.entry("mancity", "Manchester City"),
          Map.entry("manchestercity", "Manchester City"),
          Map.entry("tottenhamhotspurfc", "Tottenham Hotspur"),
          Map.entry("tottenham", "Tottenham Hotspur"),
          Map.entry("spurs", "Tottenham Hotspur"),
          Map.entry("westhamunitedfc", "West Ham United"),
          Map.entry("westham", "West Ham United"),
          Map.entry("wolverhamptonwanderersfc", "Wolverhampton Wanderers"),
          Map.entry("wolves", "Wolverhampton Wanderers"));

  private ProviderDataStandardizer() {}

  public static String leagueName(String sourceName) {
    String cleaned = clean(sourceName);
    if (cleaned == null) return null;
    return LEAGUE_ALIASES.getOrDefault(cleaned.toLowerCase(Locale.ROOT), cleaned);
  }

  public static String statisticKey(String sourceKey, String sourceName) {
    String key = clean(sourceKey);
    String candidate = key == null ? clean(sourceName) : key;
    if (candidate == null) return null;
    return STATISTIC_ALIASES.getOrDefault(aliasKey(candidate), candidate);
  }

  public static String teamName(String sourceName) {
    String cleaned = clean(sourceName);
    if (cleaned == null) return null;
    return TEAM_ALIASES.getOrDefault(aliasKey(cleaned), cleaned);
  }

  /** Folds accents and punctuation but retains club words to avoid false-positive merges. */
  public static String teamMatchKey(String sourceName) {
    String canonicalName = teamName(sourceName);
    if (canonicalName == null) return "";
    return aliasKey(canonicalName);
  }

  private static String aliasKey(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]", "");
  }

  private static String clean(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim().replaceAll("\\s+", " ");
  }
}
