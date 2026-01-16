package com.corems.common.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.security.jwt")
public class CoremsJwtProperties {
    
    private JwtAlgorithm algorithm = JwtAlgorithm.HS256;
    private String secretKey;
    private String privateKey;
    private String publicKey;
    private long refreshExpirationTimeInMinutes = 1440;
    private long accessExpirationTimeInMinutes = 10;
    private String issuer;
    private String keyId;
}

