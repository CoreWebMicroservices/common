package com.corems.common.queue.util;

import com.corems.common.queue.QueueUser;
import com.corems.common.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public final class QueueSecurityContextUtil {

    private QueueSecurityContextUtil() { }

    public static void setSecurityContextFromQueueUser(QueueUser queueUser) {
        if (queueUser == null || queueUser.getUserId() == null) {
            clearSecurityContext();
            return;
        }

        Collection<GrantedAuthority> authorities = queueUser.getRoles() != null
            ? queueUser.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList())
            : Collections.emptyList();

        UserPrincipal principal = new UserPrincipal(
            queueUser.getUserId(),
            queueUser.getEmail(),
            queueUser.getFirstName(),
            queueUser.getLastName(),
            null,
            authorities
        );

        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(principal, null, authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
