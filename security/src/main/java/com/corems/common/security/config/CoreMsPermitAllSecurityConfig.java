package com.corems.common.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Fallback security configuration that permits all requests when common security is disabled.
 * This configuration is active when spring.security.common.enabled=false.
 * <p>
 * When this config is active, no authentication is required for any endpoint.
 * This is useful for local development and testing.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@ComponentScan("com.corems.common.security")
@ConditionalOnProperty(prefix = "spring.security.common", name = "enabled", havingValue = "false")
public class CoreMsPermitAllSecurityConfig {

    @Bean
    public SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        httpSecurity.cors(cors -> {});
        httpSecurity.sessionManagement(sessionManagement -> 
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity.formLogin(AbstractHttpConfigurer::disable);
        httpSecurity.logout(AbstractHttpConfigurer::disable);
        httpSecurity.rememberMe(AbstractHttpConfigurer::disable);
        httpSecurity.httpBasic(AbstractHttpConfigurer::disable);

        httpSecurity.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        log.warn("Security is DISABLED - all endpoints are accessible without authentication");

        return httpSecurity.build();
    }
}
