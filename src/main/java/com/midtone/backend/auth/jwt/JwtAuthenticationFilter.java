package com.midtone.backend.auth.jwt;

import com.midtone.backend.auth.domain.LogoutRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final LogoutRepository logoutRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, LogoutRepository logoutRepository) {
        this.jwtProvider = jwtProvider;
        this.logoutRepository = logoutRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        extractAccessToken(request).ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length());
        return isValidAccessToken(token) ? Optional.of(token) : Optional.empty();
    }

    private boolean isValidAccessToken(String token) {
        return jwtProvider.isValid(token) && jwtProvider.isAccessToken(token) && !isLoggedOut(token);
    }

    private boolean isLoggedOut(String token) {
        return logoutRepository.findByUserId(jwtProvider.getUserId(token))
                .filter(loggedOutAt -> !jwtProvider.getIssuedAt(token).isAfter(loggedOutAt))
                .isPresent();
    }

    private void authenticate(String token) {
        long userId = jwtProvider.getUserId(token);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
