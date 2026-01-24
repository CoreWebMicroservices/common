package com.corems.common.security.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.security.config.CoremsJwtProperties;
import com.corems.common.security.config.JwtAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
public class TokenProvider {
    public static final String TOKEN_TYPE_ACCESS = "access_token";
    public static final String TOKEN_TYPE_REFRESH = "refresh_token";
    public static final String TOKEN_TYPE_ID = "id_token";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_FIRST_NAME = "first_name";
    public static final String CLAIM_LAST_NAME = "last_name";
    public static final String CLAIM_USER_ID = "user_uuid";
    public static final String CLAIM_TOKEN_ID = "token_id";
    public static final String CLAIM_ROLES = "roles";

    private final CoremsJwtProperties jwtProperties;
    private JwtParser jwtParser;
    private Key signingKey;
    private Key verificationKey;

    public TokenProvider(CoremsJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        initializeKeys();
    }

    private void initializeKeys() {
        JwtAlgorithm algorithm = jwtProperties.getAlgorithm();
        
        if (algorithm.name().startsWith("HS")) {
            if (jwtProperties.getSecretKey() == null) {
                throw new IllegalStateException("secretKey is required for HMAC algorithms");
            }
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            this.verificationKey = this.signingKey;
        } else if (algorithm.name().startsWith("RS")) {
            if (jwtProperties.getPrivateKey() != null) {
                this.signingKey = loadRSAPrivateKey(jwtProperties.getPrivateKey());
            }
            if (jwtProperties.getPublicKey() != null) {
                this.verificationKey = loadRSAPublicKey(jwtProperties.getPublicKey());
            }
            if (this.verificationKey == null) {
                throw new IllegalStateException("publicKey is required for RSA verification");
            }
        } else if (algorithm.name().startsWith("ES")) {
            throw new UnsupportedOperationException("ECDSA algorithms not yet implemented");
        }
    }

    private PrivateKey loadRSAPrivateKey(String privateKeyPem) {
        try {
            String privateKeyContent = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            
            byte[] keyBytes = Decoders.BASE64.decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }

    private PublicKey loadRSAPublicKey(String publicKeyPem) {
        try {
            String publicKeyContent = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            
            byte[] keyBytes = Decoders.BASE64.decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key", e);
        }
    }

    private JwtParser getJwtParser() {
        if (jwtParser == null) {
            if (verificationKey instanceof SecretKey secretKey) {
                jwtParser = Jwts.parser().verifyWith(secretKey).build();
            } else if (verificationKey instanceof PublicKey publicKey) {
                jwtParser = Jwts.parser().verifyWith(publicKey).build();
            } else {
                throw new IllegalStateException("Unsupported key type for verification");
            }
        }
        return jwtParser;
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaims(String token) {
        try {
            return getJwtParser().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException _) {
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Token expired");
        } catch (Exception ex) {
            log.error("Unable to get claims from token", ex);
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Unable to get claims from token");
        }
    }

    public Jws<Claims> parseToken(String token) {
        try {
            return getJwtParser().parseSignedClaims(token);
        } catch (ExpiredJwtException _) {
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Token expired");
        } catch (Exception ex) {
            log.error("Unable to parse token", ex);
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Unable to parse token");
        }
    }

    public String createAccessToken(String subject, Map<String, Object> extraClaims) {
        return createCustomToken(TOKEN_TYPE_ACCESS, subject, extraClaims, jwtProperties.getAccessExpirationTimeInMinutes());
    }

    public String createRefreshToken(String subject, Map<String, Object> extraClaims) {
        return createCustomToken(TOKEN_TYPE_REFRESH, subject, extraClaims, jwtProperties.getRefreshExpirationTimeInMinutes());
    }

    public String createIdToken(String subject, Map<String, Object> extraClaims, long expirationMinutes) {
        if (signingKey == null) {
            throw new IllegalStateException("Cannot create ID token: privateKey not configured");
        }
        return createCustomToken(TOKEN_TYPE_ID, subject, extraClaims, expirationMinutes);
    }

    public String createCustomToken(String tokenType, String subject, Map<String, Object> extraClaims, long expirationMinutes) {
        if (signingKey == null) {
            throw new IllegalStateException("Cannot create token: signing key not configured");
        }
        
        var builder = Jwts.builder()
                .header().type(tokenType).and()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMinutes * 60000));
        
        if (jwtProperties.getIssuer() != null) {
            builder.issuer(jwtProperties.getIssuer());
        }
        
        if (extraClaims != null && !extraClaims.isEmpty()) {
            builder.claims(extraClaims);
        }
        
        return builder.signWith(signingKey).compact();
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }
    
    public PublicKey getPublicKey() {
        if (verificationKey instanceof PublicKey) {
            return (PublicKey) verificationKey;
        }
        return null;
    }
    
    public String getAlgorithm() {
        return jwtProperties.getAlgorithm().name();
    }
    
    public String getKeyId() {
        if (jwtProperties.getKeyId() != null) {
            return jwtProperties.getKeyId();
        }
        return "corems-default-key";
    }
}
