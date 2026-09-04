package br.com.watchusee.watchusee.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    public Long getId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Usuário não autenticado."
            );
        }

        try {

            return Long.valueOf(
                    authentication.getName()
            );

        } catch (NumberFormatException exception) {

            throw new IllegalStateException(
                    "ID do usuário autenticado é inválido.",
                    exception
            );
        }
    }
}