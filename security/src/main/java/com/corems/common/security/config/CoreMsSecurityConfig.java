package com.corems.common.security.config;

import com.corems.common.security.filter.MdcUserFilter;
import com.corems.common.security.filter.ServiceAuthenticationFilter;
import com.corems.common.security.service.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * JWT-based security configuration for CoreMS services.
 * Active by default unless explicitly disabled via corems.security.enabled=false.
 * <p>
 * This configuration will NOT load if a service defines its own SecurityFilterChain bean,
 * allowing services to customize security while still using common security infrastructure
 * (TokenProvider, MdcUserFilter, etc.).
 * <p>
 * Provides:
 * - JWT token validation via ServiceAuthenticationFilter
 * - MDC logging context with user information
 * - Stateless session management
 * - Configurable whitelist for public endpoints
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@ConditionalOnProperty(name = "corems.security.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(SecurityFilterChain.class)
public class CoreMsSecurityConfig {

    private final MdcUserFilter mdcUserFilter;
    private final TokenProvider tokenProvider;

    @Value("${corems.security.whitelist:/actuator/health}")
    private String[] whiteListUrls;

    @Bean
    public ServiceAuthenticationFilter serviceAuthenticationFilter() {
        return new ServiceAuthenticationFilter(tokenProvider, whiteListUrls);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .rememberMe(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(whiteListUrls).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterAfter(serviceAuthenticationFilter(), CsrfFilter.class)
            .addFilterAfter(mdcUserFilter, ServiceAuthenticationFilter.class);

        log.info("CoreMS default JWT security enabled with whitelist: {}", String.join(", ", whiteListUrls));

        return http.build();
    }
}
