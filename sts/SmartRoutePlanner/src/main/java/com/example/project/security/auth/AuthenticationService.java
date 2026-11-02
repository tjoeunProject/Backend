package com.example.project.security.auth;

import java.io.IOException;
import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.security.config.JwtService;
import com.example.project.security.config.TokenRevocationService;
import com.example.project.security.token.Token;
import com.example.project.security.token.TokenRepository;
import com.example.project.security.token.TokenType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {
    private final TravelUserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRevocationService revocation;

    public AuthenticationResponse register(RegisterRequest request) {
        var user=repository.save(TravelUser.builder().nickname(request.getNickname())
                .gender(request.getGender()).email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())).build());
        return issuePair(user);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
        var user=repository.findByEmail(request.getEmail()).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if(!user.isEnabled()) throw new BadCredentialsException("Invalid credentials");
        revocation.revokeAll(user);
        return issuePair(user);
    }

    private AuthenticationResponse issuePair(TravelUser user) {
        String access=jwtService.generateToken(user), refresh=jwtService.generateRefreshToken(user);
        save(user,access); save(user,refresh);
        return AuthenticationResponse.builder().accessToken(access).refreshToken(refresh).userId(user.getId()).build();
    }

    private void save(TravelUser user,String token) {
        tokenRepository.save(Token.builder().user(user).token(token).tokenType(TokenType.BEARER)
                .expired(false).revoked(false).build());
    }

    public void refreshToken(HttpServletRequest request,HttpServletResponse response) throws IOException {
        String header=request.getHeader(HttpHeaders.AUTHORIZATION);
        if(header==null || !header.startsWith("Bearer ")) throw new BadCredentialsException("Refresh token required");
        String raw=header.substring(7);
        var stored=tokenRepository.findForUpdateByToken(raw).orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        boolean valid;
        try {
            valid=!stored.isExpired() && !stored.isRevoked() && jwtService.isRefreshToken(raw,stored.getUser());
        } catch(io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            valid=false;
        }
        if(!valid) throw new BadCredentialsException("Invalid refresh token");
        // The locked refresh row serializes concurrent reuse of this token.
        revocation.revokeAll(stored.getUser());
        var pair=issuePair(stored.getUser());
        response.setContentType("application/json");
        new ObjectMapper().writeValue(response.getOutputStream(),pair);
    }
}
