package com.example.project.security.config;

import com.example.project.security.token.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService implements LogoutHandler {
    private final TokenRepository tokenRepository;
    private final JwtService jwt;
    private final TokenRevocationService revocation;

    @Override
    public void logout(HttpServletRequest request,HttpServletResponse response,Authentication authentication) {
        String header=request.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ")) {
            var stored=tokenRepository.findByToken(header.substring(7));
            if(stored.isPresent() && !stored.get().isRevoked() && !stored.get().isExpired()) {
                try {
                    if(jwt.isAccessToken(header.substring(7),stored.get().getUser())) revocation.revokeAll(stored.get().getUser());
                } catch(io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
                    // A malformed or expired credential cannot revoke a new session.
                }
            }
        }
        SecurityContextHolder.clearContext();
    }
}
