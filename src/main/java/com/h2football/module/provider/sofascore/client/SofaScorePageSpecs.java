package com.h2football.module.provider.sofascore.client;

import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import java.util.List;
import java.util.Locale;

public final class SofaScorePageSpecs {
  private SofaScorePageSpecs() {}

  public static SofaScorePageSpec leagueCatalogue(String webUrl, String region) {
    String normalizedRegion =
        region == null || !region.matches("[A-Za-z]{2}") ? "EN" : region.toUpperCase(Locale.ROOT);
    String base = normalizeBaseUrl(webUrl);
    return new SofaScorePageSpec(
        base + "/football",
        List.of(
            SofaScoreEndpointSpec.required(
                "/api/v1/config/top-unique-tournaments/" + normalizedRegion + "/football"),
            SofaScoreEndpointSpec.pattern("^/api/v1/unique-tournament/(\\d+)/seasons$")));
  }

  public static SofaScorePageSpec standings(String webUrl, long tournamentId, long seasonId) {
    requirePositive(tournamentId, "tournamentId");
    requirePositive(seasonId, "seasonId");
    String base = normalizeBaseUrl(webUrl);
    return new SofaScorePageSpec(
        base + "/football/unique-tournament/" + tournamentId + "/season/" + seasonId + "/standings",
        List.of(
            SofaScoreEndpointSpec.required(
                "/api/v1/unique-tournament/"
                    + tournamentId
                    + "/season/"
                    + seasonId
                    + "/standings/total")));
  }

  public static SofaScorePageSpec events(
      String webUrl, long tournamentId, long seasonId, SofaScoreEventWindow window, int page) {
    requirePositive(tournamentId, "tournamentId");
    requirePositive(seasonId, "seasonId");
    if (window == null) throw new IllegalArgumentException("window must not be null");
    if (page < 0) throw new IllegalArgumentException("page must not be negative");
    String base = normalizeBaseUrl(webUrl);
    String endpoint =
        "/api/v1/unique-tournament/"
            + tournamentId
            + "/season/"
            + seasonId
            + "/events/"
            + window.pathSegment()
            + "/"
            + page;
    return new SofaScorePageSpec(
        base
            + "/football/unique-tournament/"
            + tournamentId
            + "/season/"
            + seasonId
            + "/matches/overall",
        List.of(SofaScoreEndpointSpec.required(endpoint)));
  }

  public static String eventStatisticsEndpoint(long eventId) {
    return eventEndpoint(eventId, "statistics");
  }

  public static String eventLineupsEndpoint(long eventId) {
    return eventEndpoint(eventId, "lineups");
  }

  public static String eventIncidentsEndpoint(long eventId) {
    return eventEndpoint(eventId, "incidents");
  }

  private static String eventEndpoint(long eventId, String resource) {
    requirePositive(eventId, "eventId");
    return "/api/v1/event/" + eventId + "/" + resource;
  }

  private static String normalizeBaseUrl(String value) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("SofaScore web URL must not be blank");
    String base = value.trim().replaceAll("/+$", "");
    if (!base.startsWith("https://") && !base.startsWith("http://")) {
      throw new IllegalArgumentException("SofaScore web URL must use HTTP or HTTPS");
    }
    return base;
  }

  private static void requirePositive(long value, String field) {
    if (value <= 0) throw new IllegalArgumentException(field + " must be positive");
  }
}
