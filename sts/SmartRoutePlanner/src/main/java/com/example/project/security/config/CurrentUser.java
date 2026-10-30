package com.example.project.security.config;

import java.util.Objects;
import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final TravelUserRepository users;

    public TravelUser require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return users.findByEmail(authentication.getName()).filter(TravelUser::isEnabled)
                .orElseThrow(() -> new AccessDeniedException("Active account required"));
    }

    public TravelUser requireId(Integer expectedId) {
        TravelUser user = require();
        if (!Objects.equals(user.getId(), expectedId)) {
            throw new AccessDeniedException("Resource ownership required");
        }
        return user;
    }
}
