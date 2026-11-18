package com.h2football.module.provider.sofascore.client;

import com.h2football.module.provider.sofascore.SofaScoreProperties;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class SofaScoreBrowserSessionManager {
  private final SofaScoreProperties properties;
  private Playwright playwright;
  private BrowserContext context;

  public SofaScoreBrowserSessionManager(SofaScoreProperties properties) {
    this.properties = properties;
  }

  public synchronized <T> T withPage(Function<Page, T> action) {
    if (!properties.isEnabled()) throw new IllegalStateException("SofaScore provider is disabled");
    ensureBrowser();
    Page page = context.newPage();
    try {
      return action.apply(page);
    } finally {
      if (!page.isClosed()) page.close();
    }
  }

  private void ensureBrowser() {
    if (playwright != null && context != null) return;
    try {
      Path profile = Paths.get(properties.getProfileDirectory()).toAbsolutePath().normalize();
      Files.createDirectories(profile);
      playwright = Playwright.create();
      BrowserType.LaunchPersistentContextOptions options =
          new BrowserType.LaunchPersistentContextOptions()
              .setHeadless(properties.isHeadless())
              .setIgnoreDefaultArgs(List.of("--enable-automation"))
              .setArgs(
                  List.of(
                      "--disable-blink-features=AutomationControlled",
                      "--no-first-run",
                      "--no-default-browser-check"));
      String channel = properties.getBrowserChannel();
      if (channel != null && !channel.isBlank() && !"chromium".equalsIgnoreCase(channel)) {
        options.setChannel(channel);
      }
      context = playwright.chromium().launchPersistentContext(profile, options);
      context.addInitScript(
          "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");
    } catch (Exception exception) {
      shutdown();
      throw new SofaScoreFetchException(
          "Could not initialize SofaScore browser session", exception);
    }
  }

  @PreDestroy
  public synchronized void shutdown() {
    if (context != null) {
      try {
        context.close();
      } catch (Exception ignored) {
      }
      context = null;
    }
    if (playwright != null) {
      try {
        playwright.close();
      } catch (Exception ignored) {
      }
      playwright = null;
    }
  }
}
