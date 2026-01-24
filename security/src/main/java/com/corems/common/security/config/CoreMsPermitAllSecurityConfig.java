package com.corems.common.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permissive security configuration for local development and testing.
 * Active only when corems.security.enabled=false.
 * <p>
 * This configuration will NOT load if a service defines its own SecurityFilterChain bean,
 * allowing services to customize security while still using common security infrastructure.
 * <p>
 * WARNING: All endpoints are publicly accessible without authentication.
 * Never use this configuration in production environments.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "corems.security.enabled", havingValue = "false")
@ConditionalOnMissingBean(SecurityFilterChain.class)
public class CoreMsPermitAllSecurityConfig {

    @Bean
    public SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .rememberMe(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        log.warn("⚠️  CoreMS SECURITY DISABLED - All endpoints accessible without authentication");

        return http.build();
    }
}
