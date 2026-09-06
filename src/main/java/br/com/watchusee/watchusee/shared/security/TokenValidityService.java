package br.com.watchusee.watchusee.shared.security;

import java.time.Instant;

public interface TokenValidityService {

    boolean isTokenValid(Long userId, Instant issuedAt);
}
