package com.example.project.security.config;

import com.example.project.member.domain.TravelUser;
import com.example.project.security.token.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {
    private final TokenRepository tokens;

    @Transactional
    public void revokeAll(TravelUser user) {
        var active=tokens.findAllValidTokenByUser(user.getId());
        active.forEach(token -> { token.setExpired(true); token.setRevoked(true); });
        tokens.saveAll(active);
    }
}
