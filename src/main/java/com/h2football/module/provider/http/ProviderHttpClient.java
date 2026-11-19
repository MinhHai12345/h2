package com.h2football.module.provider.http;

import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProviderHttpClient {
  private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024;
  private final WebClient.Builder builder;

  public ProviderHttpClient(WebClient.Builder builder) {
    this.builder = builder;
  }

  public <T> T getJson(
      String baseUrl,
      String path,
      Map<String, String> headers,
      Class<T> responseType,
      Duration timeout,
      Object... variables) {
    return get(baseUrl, path, headers, responseType, timeout, variables);
  }

  public String getText(
      String baseUrl,
      String path,
      Map<String, String> headers,
      Duration timeout,
      Object... variables) {
    return get(baseUrl, path, headers, String.class, timeout, variables);
  }

  public byte[] getBytes(
      String baseUrl,
      String path,
      Map<String, String> headers,
      Duration timeout,
      Object... variables) {
    return get(baseUrl, path, headers, byte[].class, timeout, variables);
  }

  private <T> T get(
      String baseUrl,
      String path,
      Map<String, String> headers,
      Class<T> responseType,
      Duration timeout,
      Object... variables) {
    WebClient client =
        builder
            .clone()
            .baseUrl(baseUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
            .build();
    WebClient.RequestHeadersSpec<?> request = client.get().uri(path, variables);
    headers.forEach(request::header);
    return request
        .retrieve()
        .onStatus(
            status -> status.isError(),
            response ->
                response
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(
                        body ->
                            new ProviderHttpException(
                                response.statusCode(),
                                "Provider HTTP "
                                    + response.statusCode().value()
                                    + " for "
                                    + path
                                    + (body.isBlank() ? "" : ": " + preview(body)))))
        .bodyToMono(responseType)
        .timeout(timeout)
        .block();
  }

  private static String preview(String body) {
    String normalized = body.replaceAll("\\s+", " ").trim();
    return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
  }
}
