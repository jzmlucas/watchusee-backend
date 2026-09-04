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
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        log.info(
                "JWT Filter - {} {} - Authorization presente: {}",
                request.getMethod(),
                request.getRequestURI(),
                authorizationHeader != null
        );

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            log.warn(
                    "JWT Filter - Token ausente para {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader
                        .substring(7)
                        .trim();

        if (token.isBlank()) {

            log.warn(
                    "JWT Filter - Token vazio para {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isValid(token)) {

            log.warn(
                    "JWT Filter - Token inválido para {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        try {

            Long userId =
                    jwtService.extractUserId(token);

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

            log.info(
                    "JWT Filter - Usuário autenticado com sucesso. userId={}",
                    userId
            );

        } catch (Exception exception) {

            SecurityContextHolder.clearContext();

            log.error(
                    "JWT Filter - Erro ao processar token",
                    exception
            );
        }

        filterChain.doFilter(request, response);
    }
}