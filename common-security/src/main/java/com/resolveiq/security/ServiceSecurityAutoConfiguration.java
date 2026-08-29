package com.resolveiq.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@EnableMethodSecurity
@Import(ProductionSecurityGuard.class)
public class ServiceSecurityAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    JwtService jwtService(
        @Value("${resolveiq.jwt.secret:fictional_jwt_hmac_secret_key_minimum_256_bits_for_local_development_only_12345}") String secret,
        @Value("${resolveiq.jwt.issuer:resolveiq-auth}") String issuer,
        @Value("${resolveiq.jwt.audience:resolveiq-api}") String audience
    ) { return new JwtService(secret, issuer, audience); }

    @Bean @ConditionalOnMissingBean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) { return new JwtAuthenticationFilter(jwtService); }

    @Bean @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain serviceSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter filter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/api/v1/agent/**").hasAnyRole("AGENT", "ADMIN")
                .requestMatchers("/api/v1/knowledge/**").hasAnyRole("KNOWLEDGE_MANAGER", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
