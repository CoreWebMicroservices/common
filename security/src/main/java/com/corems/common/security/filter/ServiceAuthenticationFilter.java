package com.corems.common.security.filter;

import com.corems.common.security.UserPrincipal;
import com.corems.common.security.service.TokenProvider;
import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final String[] whiteListUrls;
    private final AntPathMatcher pathMatcher;

    public ServiceAuthenticationFilter(TokenProvider tokenProvider, String[] whiteListUrls) {
        this.tokenProvider = tokenProvider;
        this.whiteListUrls = whiteListUrls;
        this.pathMatcher = new AntPathMatcher();
    }

    private String getJWTFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring("Bearer ".length());
        }

        return null;
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return Arrays.stream(whiteListUrls)
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Skip authentication for whitelisted URLs
        if (isWhitelisted(request)) {
            log.debug("Skipping authentication for whitelisted URL: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = getJWTFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            Jws<Claims> parsed = tokenProvider.parseToken(jwt);
            Header header = parsed.getHeader();
            if (!Objects.equals(header.getType(), TokenProvider.TOKEN_TYPE_ACCESS)) {
                throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Provided token has wrong type");
            }

            Claims claims = parsed.getPayload();

            Object rolesObj = claims.get(TokenProvider.CLAIM_ROLES);
            List<String> roles;
            if (rolesObj instanceof List) {
                roles = ((List<?>) rolesObj).stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toList();
            } else {
                roles = List.of();
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UserPrincipal principal = new UserPrincipal(
                    claims.getSubject(),
                    claims.get(TokenProvider.CLAIM_EMAIL, String.class),
                    claims.get(TokenProvider.CLAIM_FIRST_NAME, String.class),
                    claims.get(TokenProvider.CLAIM_LAST_NAME, String.class),
                    claims.get(TokenProvider.CLAIM_TOKEN_ID, String.class),
                    authorities
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "No token provided");
        }

        filterChain.doFilter(request, response);
    }

}
