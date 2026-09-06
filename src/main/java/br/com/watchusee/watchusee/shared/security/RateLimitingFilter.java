package br.com.watchusee.watchusee.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static class Window {
        final long windowStartEpochSeconds;
        final AtomicInteger count;

        Window(long windowStartEpochSeconds) {
            this.windowStartEpochSeconds = windowStartEpochSeconds;
            this.count = new AtomicInteger(0);
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${security.rate-limit.auth.max-requests:10}")
    private int authMaxRequests;

    @Value("${security.rate-limit.auth.window-seconds:60}")
    private int authWindowSeconds;

    @Value("${security.rate-limit.public-api.max-requests:60}")
    private int publicApiMaxRequests;

    @Value("${security.rate-limit.public-api.window-seconds:60}")
    private int publicApiWindowSeconds;

    @Value("${security.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = resolveRule(request);

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = rule.bucketName() + ":" + clientIp;

        long nowEpochSeconds = Instant.now().getEpochSecond();
        long currentWindowStart =
                nowEpochSeconds - (nowEpochSeconds % rule.windowSeconds());

        Window window = windows.compute(key, (k, existing) -> {

            if (existing == null ||
                    existing.windowStartEpochSeconds != currentWindowStart) {
                return new Window(currentWindowStart);
            }

            return existing;
        });

        int currentCount = window.count.incrementAndGet();

        if (currentCount > rule.maxRequests()) {

            log.warn(
                    "Rate limit excedido. ip={} rota={} limite={}",
                    clientIp,
                    rule.bucketName(),
                    rule.maxRequests()
            );

            writeTooManyRequests(response, request, rule);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        boolean isAuthEndpoint =
                uri.startsWith("/api/v1/auth/login") ||
                        ("POST".equalsIgnoreCase(method) && uri.equals("/api/v1/users")) ||
                        ("PUT".equalsIgnoreCase(method) && uri.equals("/api/v1/users/me/password"));

        if (isAuthEndpoint) {
            return new RateLimitRule(
                    "auth",
                    authMaxRequests,
                    authWindowSeconds
            );
        }

        boolean isPublicMovieEndpoint =
                uri.startsWith("/api/v1/movies");

        if (isPublicMovieEndpoint) {
            return new RateLimitRule(
                    "public-api",
                    publicApiMaxRequests,
                    publicApiWindowSeconds
            );
        }

        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(
            HttpServletResponse response,
            HttpServletRequest request,
            RateLimitRule rule
    ) throws IOException {

        response.setStatus(429); // HttpStatus.TOO_MANY_REQUESTS
        response.setHeader("Retry-After", String.valueOf(rule.windowSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 429,
                "error", "Too Many Requests",
                "message", "Muitas requisições em um curto período. Tente novamente em instantes.",
                "path", request.getRequestURI()
        );

        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }

    private record RateLimitRule(
            String bucketName,
            int maxRequests,
            int windowSeconds
    ) {
    }
}
