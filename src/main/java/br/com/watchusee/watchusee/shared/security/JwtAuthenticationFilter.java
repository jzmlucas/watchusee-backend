package br.com.watchusee.watchusee.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenValidityService tokenValidityService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            TokenValidityService tokenValidityService
    ) {
        this.jwtService = jwtService;
        this.tokenValidityService = tokenValidityService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        log.debug(
                "JWT Filter - {} {} - Authorization presente: {}",
                request.getMethod(),
                request.getRequestURI(),
                authorizationHeader != null
        );

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith(BEARER_PREFIX)) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader
                        .substring(BEARER_PREFIX.length())
                        .trim();

        if (token.isBlank()) {

            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        try {

            if (!jwtService.isValid(token)) {

                log.debug(
                        "JWT Filter - Token inválido, expirado, revogado ou com audience inválida. {} {}",
                        request.getMethod(),
                        request.getRequestURI()
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(request, response);
                return;
            }

            Long userId =
                    jwtService.extractUserId(token);

            Instant issuedAt =
                    jwtService.extractIssuedAt(token);

            if (!tokenValidityService.isTokenValid(userId, issuedAt)) {

                log.debug(
                        "JWT Filter - Token emitido antes da invalidação. userId={}",
                        userId
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                String.valueOf(userId),
                                null,
                                Collections.emptyList()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                log.debug(
                        "JWT Filter - Usuário autenticado com sucesso. userId={}",
                        userId
                );
            }

        } catch (Exception exception) {

            SecurityContextHolder.clearContext();

            log.debug(
                    "JWT Filter - Token rejeitado: {}",
                    exception.getMessage()
            );
        }

        filterChain.doFilter(request, response);
    }
}