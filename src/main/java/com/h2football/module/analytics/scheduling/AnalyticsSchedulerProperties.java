package com.h2football.module.analytics.scheduling;

import com.h2football.module.identity.domain.model.Provider;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.analytics.scheduler")
public class AnalyticsSchedulerProperties {
  private boolean enabled = true;
  private List<Provider> providers = List.of(Provider.SOFASCORE);

  @Min(1)
  private int lookaheadHours = 72;

  @Min(1)
  private int refreshAfterMinutes = 360;

  @NotBlank private String matchRefreshCron = "0 0/30 * * * ?";

  @NotBlank private String modelEvaluationCron = "0 0 2 * * ?";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<Provider> getProviders() {
    return providers;
  }

  public void setProviders(List<Provider> providers) {
    this.providers = providers == null ? List.of() : List.copyOf(providers);
  }

  public int getLookaheadHours() {
    return lookaheadHours;
  }

  public void setLookaheadHours(int lookaheadHours) {
    this.lookaheadHours = lookaheadHours;
  }

  public int getRefreshAfterMinutes() {
    return refreshAfterMinutes;
  }

  public void setRefreshAfterMinutes(int refreshAfterMinutes) {
    this.refreshAfterMinutes = refreshAfterMinutes;
  }

  public String getMatchRefreshCron() {
    return matchRefreshCron;
  }

  public void setMatchRefreshCron(String matchRefreshCron) {
    this.matchRefreshCron = matchRefreshCron;
  }

  public String getModelEvaluationCron() {
    return modelEvaluationCron;
  }

  public void setModelEvaluationCron(String modelEvaluationCron) {
    this.modelEvaluationCron = modelEvaluationCron;
  }
}
