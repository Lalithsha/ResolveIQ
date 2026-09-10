package com.resolveiq.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
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

    @Bean @ConditionalOnMissingBean
    com.resolveiq.contracts.flags.FeatureFlagStore featureFlagStore(
        @org.springframework.beans.factory.annotation.Autowired(required = false) javax.sql.DataSource dataSource
    ) {
        if (dataSource != null) {
            return new com.resolveiq.security.flags.JdbcFeatureFlagStore(dataSource);
        }
        return null;
    }

    @Bean @ConditionalOnMissingBean
    com.resolveiq.contracts.flags.FeatureFlagService featureFlagService(
        @org.springframework.beans.factory.annotation.Autowired(required = false) com.resolveiq.contracts.flags.FeatureFlagStore store
    ) {
        return new com.resolveiq.contracts.flags.FeatureFlagService(store, false);
    }

    @Bean @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain serviceSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter filter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info", "/error", "/v3/api-docs/**").permitAll()
                .requestMatchers("/webhooks/v1/email/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").hasAnyRole("ADMIN", "AUDITOR")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/agent/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN", "AUDITOR")
                .requestMatchers("/api/v1/agent/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidents/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN", "AUDITOR")
                .requestMatchers("/api/v1/incidents/**").hasAnyRole("TEAM_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/resolution-actions/**", "/api/v1/tickets/*/resolution-actions/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN", "AUDITOR")
                .requestMatchers("/api/v1/resolution-actions/**", "/api/v1/tickets/*/resolution-actions/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/**").hasAnyRole("KNOWLEDGE_MANAGER", "ADMIN", "AUDITOR", "AGENT", "TEAM_LEAD")
                .requestMatchers("/api/v1/knowledge/**").hasAnyRole("KNOWLEDGE_MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/governance/**").hasAnyRole("ADMIN", "AUDITOR", "TEAM_LEAD")
                .requestMatchers("/api/v1/governance/**").hasAnyRole("ADMIN", "TEAM_LEAD")
                .requestMatchers(HttpMethod.GET, "/api/v1/conversations/**", "/api/v1/deliveries/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN", "AUDITOR", "CUSTOMER")
                .requestMatchers("/api/v1/conversations/**", "/api/v1/deliveries/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/v1/evidence/**", "/api/v1/tickets/*/evidence/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN", "AUDITOR", "CUSTOMER")
                .requestMatchers("/api/v1/evidence/**", "/api/v1/tickets/*/evidence/**").hasAnyRole("AGENT", "TEAM_LEAD", "ADMIN", "CUSTOMER")
                .requestMatchers("/api/v1/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
