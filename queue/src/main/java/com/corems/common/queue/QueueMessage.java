package com.corems.common.queue;

import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Setter
@Getter
public class QueueMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private String id = UUID.randomUUID().toString();
    private String type;
    private Object payload;
    private int attempts = 0;
    private Map<String, String> headers = new HashMap<>();
    private Instant createdAt = Instant.now();
    private Instant lastAttemptAt;
    private String sourceService;
    private String correlationId;
    private Integer priority = 0;
    private Instant expiresAt;
    private QueueUser user;
    
    public void incrementAttempts() {
        this.attempts++;
        this.lastAttemptAt = Instant.now();
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    public QueueMessage withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
    
    public QueueMessage withPriority(int priority) {
        this.priority = priority;
        return this;
    }
    
    public QueueMessage withHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }
    
    public QueueMessage withUser(QueueUser user) {
        this.user = user;
        return this;
    }
    
    public QueueMessage withUserFromSecurityContext() {
        Optional<UserPrincipal> userPrincipal = SecurityUtils.getUserPrincipalOptional();
        if (userPrincipal.isPresent()) {
            UserPrincipal principal = userPrincipal.get();
            if (principal.getUserId() != null) {
                this.user = new QueueUser(
                    principal.getUserId(),
                    principal.getEmail(),
                    principal.getFirstName(),
                    principal.getLastName(),
                    principal.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toList())
                );
            }
        }
        return this;
    }
}

