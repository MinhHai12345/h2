package com.h2football.module.provider.sofascore;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.providers.sofascore")
public class SofaScoreProperties {
    private boolean enabled;
    private String webUrl = "https://www.sofascore.com";
    private String region = "EN";
    private boolean headless = true;
    private String browserChannel = "chrome";
    private String profileDirectory = ".runtime/sofascore-profile";
    private int navigationTimeoutMillis = 60000;
    private int responseTimeoutMillis = 20000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getWebUrl() { return webUrl; }
    public void setWebUrl(String webUrl) { this.webUrl = webUrl; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public boolean isHeadless() { return headless; }
    public void setHeadless(boolean headless) { this.headless = headless; }
    public String getBrowserChannel() { return browserChannel; }
    public void setBrowserChannel(String browserChannel) { this.browserChannel = browserChannel; }
    public String getProfileDirectory() { return profileDirectory; }
    public void setProfileDirectory(String profileDirectory) { this.profileDirectory = profileDirectory; }
    public int getNavigationTimeoutMillis() { return navigationTimeoutMillis; }
    public void setNavigationTimeoutMillis(int navigationTimeoutMillis) { this.navigationTimeoutMillis = navigationTimeoutMillis; }
    public int getResponseTimeoutMillis() { return responseTimeoutMillis; }
    public void setResponseTimeoutMillis(int responseTimeoutMillis) { this.responseTimeoutMillis = responseTimeoutMillis; }
}
