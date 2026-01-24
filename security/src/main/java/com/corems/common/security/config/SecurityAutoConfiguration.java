package com.corems.common.security.config;

import com.corems.common.security.service.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Core autoconfiguration for security components.
 * Enables JWT properties and scans security package for filters and services.
 * <p>
 * This configuration is always active and provides the foundation for both
 * CoreMsSecurityConfig (JWT-enabled) and CoreMsPermitAllSecurityConfig (development mode).
 */
@Slf4j
@Configuration
@ComponentScan("com.corems.common.security")
@EnableConfigurationProperties(CoremsJwtProperties.class)
public class SecurityAutoConfiguration {

    public SecurityAutoConfiguration() {
        log.debug("CoreMS Security AutoConfiguration initialized");
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenProvider tokenProvider(CoremsJwtProperties jwtProperties) {
        log.debug("Creating TokenProvider bean with algorithm: {}", jwtProperties.getAlgorithm());
        return new TokenProvider(jwtProperties);
    }
}
