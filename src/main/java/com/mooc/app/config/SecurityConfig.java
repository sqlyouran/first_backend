package com.mooc.app.config;

import com.mooc.app.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    CorsConfigurationSource corsConfigurationSource,
                                                    JwtAuthenticationFilter jwtAuthenticationFilter,
                                                    AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Admin/management endpoints require authentication (must be before public spot patterns)
                .requestMatchers("/api/spots/stale", "/api/spots/enrichment/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/ai/knowledge/**").authenticated()
                // Auth endpoints
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/send-code",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout").permitAll()
                // Public read endpoints
                .requestMatchers(HttpMethod.GET,
                        "/api/posts", "/api/posts/*",
                        "/api/posts/*/comments",
                        "/api/posts/*/vote-stats",
                        "/api/posts/*/bookmark-status",
                        "/api/comments/*/replies",
                        "/api/cities/**",
                        "/api/search/**",
                        "/api/services/**",
                        "/api/hello").permitAll()
                // Spots: public detail/list but NOT enrichment admin endpoints
                .requestMatchers(HttpMethod.GET,
                        "/api/spots", "/api/spots/*",
                        "/api/spots/*/posts",
                        "/api/spots/*/comments",
                        "/api/spots/*/bookmark-status",
                        "/api/spots/ranking").permitAll()
                // Public user profile and posts (by username/id)
                .requestMatchers(HttpMethod.GET, "/api/users/*", "/api/users/*/posts").permitAll()
                // AI chat: permitAll at security layer (rate limiting handled in controller)
                .requestMatchers("/api/ai/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new ForwardedHeaderFilter(), jwtAuthenticationFilter.getClass())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new JsonAuthenticationEntryPoint();
    }
}
