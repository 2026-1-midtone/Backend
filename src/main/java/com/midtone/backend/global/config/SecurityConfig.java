package com.midtone.backend.global.config;

import com.midtone.backend.auth.domain.LogoutRepository;
import com.midtone.backend.auth.jwt.JwtAuthenticationFilter;
import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    @Bean
    @Profile("!local")
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            JwtProvider jwtProvider,
            LogoutRepository logoutRepository) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) ->
                        writeUnauthorizedResponse(response, objectMapper)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, logoutRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Profile("local")
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, ObjectMapper objectMapper) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse("로그인이 필요합니다."));
    }
}
