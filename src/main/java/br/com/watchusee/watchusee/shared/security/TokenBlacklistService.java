package br.com.watchusee.watchusee.shared.security;

import java.time.Instant;


public interface TokenBlacklistService {


    void revoke(String jti, Instant expiresAt);

    boolean isRevoked(String jti);
}
