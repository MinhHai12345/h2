package com.h2football.module.provider.sofascore.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2football.module.provider.sofascore.SofaScoreProperties;
import com.h2football.module.provider.sofascore.dto.SofaScoreDtos;
import com.h2football.module.provider.sofascore.dto.SofaScoreLeagueCatalogue;
import com.h2football.module.provider.sofascore.model.SofaScoreEventWindow;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreClientImpl implements SofaScoreClient {
  private static final int POLL_INTERVAL_MILLIS = 100;
  private final SofaScoreBrowserSessionManager browser;
  private final SofaScoreProperties properties;
  private final ObjectMapper objectMapper;

  public SofaScoreClientImpl(
      SofaScoreBrowserSessionManager browser,
      SofaScoreProperties properties,
      ObjectMapper objectMapper) {
    this.browser = browser;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public SofaScoreLeagueCatalogue fetchLeagueCatalogue(List<Long> selectedTournamentIds) {
    SofaScorePageSpec spec =
        SofaScorePageSpecs.leagueCatalogue(properties.getWebUrl(), properties.getRegion());
    return browser.withPage(page -> captureCatalogue(page, spec, selectedTournamentIds));
  }

  private SofaScoreLeagueCatalogue captureCatalogue(
      Page page, SofaScorePageSpec spec, List<Long> selectedTournamentIds) {
    SofaScoreEndpointSpec catalogueEndpoint = spec.requiredEndpoint();
    SofaScoreEndpointSpec seasonPattern =
        spec.endpoints().stream().filter(SofaScoreEndpointSpec::dynamic).findFirst().orElseThrow();
    AtomicReference<Response> catalogueResponse = new AtomicReference<>();
    Map<Long, Response> seasonResponses = new ConcurrentHashMap<>();
    Consumer<Response> listener =
        response -> {
          if (catalogueEndpoint.matches(response.url())) {
            catalogueResponse.compareAndSet(null, response);
            return;
          }
          Long tournamentId = seasonPattern.captureLong(response.url(), 1);
          if (tournamentId != null && selected(selectedTournamentIds, tournamentId)) {
            seasonResponses.putIfAbsent(tournamentId, response);
          }
        };
    page.onResponse(listener);
    try {
      navigate(page, spec);
      waitUntil(page, () -> catalogueResponse.get() != null);
      Response captured = catalogueResponse.get();
      if (captured == null || !captured.ok()) {
        throw new SofaScoreFetchException("SofaScore tournament catalogue request failed");
      }
      SofaScoreDtos.TournamentCatalogue payload =
          objectMapper.readValue(captured.text(), SofaScoreDtos.TournamentCatalogue.class);
      List<SofaScoreDtos.Tournament> tournaments =
          payload.uniqueTournaments() == null
              ? List.of()
              : payload.uniqueTournaments().stream()
                  .filter(java.util.Objects::nonNull)
                  .filter(t -> t.id() != null && t.id() > 0)
                  .filter(t -> selected(selectedTournamentIds, t.id()))
                  .toList();
      waitUntil(
          page,
          () ->
              seasonResponses
                  .keySet()
                  .containsAll(tournaments.stream().map(SofaScoreDtos.Tournament::id).toList()));
      Map<Long, List<SofaScoreDtos.Season>> seasons = new LinkedHashMap<>();
      for (SofaScoreDtos.Tournament tournament : tournaments) {
        Response seasonResponse = seasonResponses.get(tournament.id());
        if (seasonResponse == null || !seasonResponse.ok()) continue;
        SofaScoreDtos.SeasonsResponse seasonPayload =
            objectMapper.readValue(seasonResponse.text(), SofaScoreDtos.SeasonsResponse.class);
        seasons.put(
            tournament.id(), seasonPayload.seasons() == null ? List.of() : seasonPayload.seasons());
      }
      return new SofaScoreLeagueCatalogue(tournaments, seasons);
    } catch (SofaScoreFetchException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new SofaScoreFetchException("Failed to capture SofaScore league catalogue", exception);
    } finally {
      page.offResponse(listener);
    }
  }

  @Override
  public List<SofaScoreDtos.StandingGroup> fetchStandings(long tournamentId, long seasonId) {
    SofaScorePageSpec spec =
        SofaScorePageSpecs.standings(properties.getWebUrl(), tournamentId, seasonId);
    SofaScoreDtos.StandingsResponse payload =
        browser.withPage(
            page ->
                captureJson(
                    page,
                    spec,
                    SofaScoreDtos.StandingsResponse.class,
                    "SofaScore standings request failed"));
    return payload.standings() == null ? List.of() : payload.standings();
  }

  @Override
  public List<SofaScoreDtos.Event> fetchEvents(
      long tournamentId, long seasonId, SofaScoreEventWindow window, int pageNumber) {
    SofaScorePageSpec spec =
        SofaScorePageSpecs.events(
            properties.getWebUrl(), tournamentId, seasonId, window, pageNumber);
    SofaScoreDtos.EventsResponse payload =
        browser.withPage(
            page ->
                captureJson(
                    page,
                    spec,
                    SofaScoreDtos.EventsResponse.class,
                    "SofaScore " + window.pathSegment() + " events request failed"));
    return payload.events() == null ? List.of() : payload.events();
  }

  private <T> T captureJson(
      Page page, SofaScorePageSpec spec, Class<T> responseType, String failureMessage) {
    AtomicReference<Response> captured = new AtomicReference<>();
    SofaScoreEndpointSpec endpoint = spec.requiredEndpoint();
    Consumer<Response> listener =
        response -> {
          if (endpoint.matches(response.url())) captured.compareAndSet(null, response);
        };
    page.onResponse(listener);
    try {
      navigate(page, spec);
      waitUntil(page, () -> captured.get() != null);
      Response response = captured.get();
      if (response == null || !response.ok()) throw new SofaScoreFetchException(failureMessage);
      return objectMapper.readValue(response.text(), responseType);
    } catch (SofaScoreFetchException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new SofaScoreFetchException("Failed to capture SofaScore response", exception);
    } finally {
      page.offResponse(listener);
    }
  }

  private void navigate(Page page, SofaScorePageSpec spec) {
    page.navigate(
        spec.loadPageUrl(),
        new Page.NavigateOptions()
            .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
            .setTimeout(properties.getNavigationTimeoutMillis()));
  }

  private void waitUntil(Page page, java.util.function.BooleanSupplier condition) {
    long deadline =
        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(properties.getResponseTimeoutMillis());
    while (!condition.getAsBoolean() && System.nanoTime() < deadline && !page.isClosed()) {
      page.waitForTimeout(POLL_INTERVAL_MILLIS);
    }
  }

  private boolean selected(List<Long> ids, Long id) {
    return ids == null || ids.isEmpty() || ids.contains(id);
  }
}
