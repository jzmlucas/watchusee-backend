package br.com.watchusee.watchusee.config;

import java.util.Set;

public record IpAllowlistConfig(
    Set<String> allowedHosts,

    boolean logViolationAttempts
) {
    public IpAllowlistConfig(Set<String> allowedHosts, boolean logViolationAttempts) {
        this.allowedHosts = allowedHosts;
        this.logViolationAttempts = logViolationAttempts;
    }

    public boolean isAllowed(String host) {
        return allowedHosts != null && allowedHosts.contains(host.toLowerCase());
    }

    public static String extractHostFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getHost() != null ? uri.getHost().toLowerCase() : "";
        } catch (Exception e) {
            return "";
        }
    }
}