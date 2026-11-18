package com.h2football.module.provider.sofascore.client;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SofaScoreEndpointSpec(String matcher, boolean required, Pattern pattern) {
    public SofaScoreEndpointSpec(String matcher, boolean required) {
        this(matcher, required, null);
    }
    public static SofaScoreEndpointSpec required(String path) {
        return new SofaScoreEndpointSpec(path, true);
    }
    public static SofaScoreEndpointSpec pattern(String regex) {
        return new SofaScoreEndpointSpec(regex, false, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }
    public boolean dynamic() { return pattern != null; }
    public boolean matches(String responseUrl) {
        String path = responsePath(responseUrl);
        return path != null && (dynamic() ? pattern.matcher(path).matches() : path.endsWith(normalize(matcher)));
    }
    public Long captureLong(String responseUrl, int group) {
        if (!dynamic()) return null;
        try {
            Matcher result = pattern.matcher(responsePath(responseUrl));
            return result.matches() && group > 0 && group <= result.groupCount()
                    ? Long.valueOf(result.group(group)) : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }
    private static String responsePath(String url) {
        try { return normalize(URI.create(url).getRawPath()); }
        catch (RuntimeException exception) { return null; }
    }
    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "/";
        String path = value.trim();
        while (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return (path.startsWith("/") ? path : "/" + path).toLowerCase(Locale.ROOT);
    }
}
