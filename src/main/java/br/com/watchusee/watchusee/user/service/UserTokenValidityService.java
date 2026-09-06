package br.com.watchusee.watchusee.user.service;

import br.com.watchusee.watchusee.shared.security.TokenValidityService;
import br.com.watchusee.watchusee.user.domain.User;
import br.com.watchusee.watchusee.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserTokenValidityService implements TokenValidityService {

    private final UserRepository userRepository;

    public UserTokenValidityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenValid(Long userId, Instant issuedAt) {

        if (userId == null) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user -> user.isTokenValid(issuedAt))
                .orElse(false);
    }
}
