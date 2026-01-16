package com.corems.common.security.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.config.CoremsJwtProperties;
import com.corems.common.security.config.JwtAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenProviderTest {

    private TokenProvider tokenProvider;
    private final String testSecretKey = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGVzdGluZy1wdXJwb3Nlcy1vbmx5LXRoaXMtaXMtYS12ZXJ5LWxvbmctc2VjcmV0LWtleQ==";
    private final long accessExpiration = 60; // 1 hour in minutes
    private final long refreshExpiration = 1440; // 24 hours in minutes

    @BeforeEach
    void setUp() {
        CoremsJwtProperties jwtProperties = new CoremsJwtProperties();
        jwtProperties.setAlgorithm(JwtAlgorithm.HS256);
        jwtProperties.setSecretKey(testSecretKey);
        jwtProperties.setAccessExpirationTimeInMinutes(accessExpiration);
        jwtProperties.setRefreshExpirationTimeInMinutes(refreshExpiration);
        jwtProperties.setIssuer("http://localhost:3000");
        jwtProperties.setKeyId("test-key-1");
        
        tokenProvider = new TokenProvider(jwtProperties);
    }

    @Test
    void createAccessToken_WithValidData_CreatesToken() {
        String subject = "123e4567-e89b-12d3-a456-426614174000";
        Map<String, Object> claims = Map.of(
            TokenProvider.CLAIM_FIRST_NAME, "John",
            TokenProvider.CLAIM_LAST_NAME, "Doe",
            TokenProvider.CLAIM_ROLES, List.of("USER_MS_USER")
        );

        String token = tokenProvider.createAccessToken(subject, claims);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void createRefreshToken_WithValidData_CreatesToken() {
        String subject = "123e4567-e89b-12d3-a456-426614174000";
        Map<String, Object> claims = Map.of(
            TokenProvider.CLAIM_TOKEN_ID, "token-uuid-123"
        );

        String token = tokenProvider.createRefreshToken(subject, claims);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void getAllClaims_WithValidToken_ReturnsClaims() {
        String subject = "123e4567-e89b-12d3-a456-426614174000";
        Map<String, Object> expectedClaims = Map.of(
            TokenProvider.CLAIM_FIRST_NAME, "John",
            TokenProvider.CLAIM_LAST_NAME, "Doe"
        );
        
        String token = tokenProvider.createAccessToken(subject, expectedClaims);
        
        Claims claims = tokenProvider.getAllClaims(token);
        
        assertEquals(subject, claims.getSubject());
        assertEquals("John", claims.get(TokenProvider.CLAIM_FIRST_NAME));
        assertEquals("Doe", claims.get(TokenProvider.CLAIM_LAST_NAME));
    }

    @Test
    void getAllClaims_WithInvalidToken_ThrowsAuthServiceException() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(ServiceException.class, () -> tokenProvider.getAllClaims(invalidToken));
    }

    @Test
    void getAllClaims_WithExpiredToken_ThrowsAuthServiceException() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKey));
        String expiredToken = Jwts.builder()
            .subject("test@example.com")
            .issuedAt(new Date(System.currentTimeMillis() - 7200000))
            .expiration(new Date(System.currentTimeMillis() - 3600000))
            .signWith(key)
            .compact();
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> tokenProvider.getAllClaims(expiredToken));
        assertTrue(exception.getErrors().get(0).getDetails().contains("Token expired"));
    }

    @Test
    void getClaim_WithValidTokenAndClaimResolver_ReturnsClaimValue() {
        String subject = "test-user-id";
        Map<String, Object> claims = Map.of(TokenProvider.CLAIM_FIRST_NAME, "John");
        String token = tokenProvider.createAccessToken(subject, claims);
        
        String firstName = tokenProvider.getClaim(token, claims1 -> claims1.get(TokenProvider.CLAIM_FIRST_NAME, String.class));
        
        assertEquals("John", firstName);
    }

    @Test
    void parseToken_WithValidToken_ReturnsJws() {
        String subject = "test-user-id";
        Map<String, Object> claims = Map.of(TokenProvider.CLAIM_FIRST_NAME, "John");
        String token = tokenProvider.createAccessToken(subject, claims);
        
        Jws<Claims> jws = tokenProvider.parseToken(token);
        
        assertNotNull(jws);
        assertEquals(subject, jws.getPayload().getSubject());
    }

    @Test
    void parseToken_WithInvalidToken_ThrowsAuthServiceException() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(ServiceException.class, () -> tokenProvider.parseToken(invalidToken));
    }

    @Test
    void isTokenValid_WithValidToken_ReturnsTrue() {
        String subject = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        String token = tokenProvider.createAccessToken(subject, claims);
        
        boolean isValid = tokenProvider.isTokenValid(token);
        
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_WithExpiredToken_ReturnsFalse() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKey));
        String expiredToken = Jwts.builder()
            .subject("test@example.com")
            .issuedAt(new Date(System.currentTimeMillis() - 7200000))
            .expiration(new Date(System.currentTimeMillis() - 3600000))
            .signWith(key)
            .compact();
        
        assertThrows(ServiceException.class, () -> tokenProvider.isTokenValid(expiredToken));
    }

    @Test
    void createAccessToken_WithEmptySubject_CreatesTokenWithEmptySubject() {
        String subject = "";
        Map<String, Object> claims = Map.of(TokenProvider.CLAIM_FIRST_NAME, "John");
        
        String token = tokenProvider.createAccessToken(subject, claims);
        
        assertNotNull(token);
        Claims tokenClaims = tokenProvider.getAllClaims(token);
        assertNull(tokenClaims.getSubject());
    }

    @Test
    void createAccessToken_WithNullClaims_CreatesToken() {
        String subject = "test-user-id";
        
        String token = tokenProvider.createAccessToken(subject, null);
        
        assertNotNull(token);
        Claims tokenClaims = tokenProvider.getAllClaims(token);
        assertEquals(subject, tokenClaims.getSubject());
    }

    @Test
    void createTokens_WithDifferentExpirationTimes_HaveDifferentExpirations() {
        String subject = "test-user-id";
        Map<String, Object> claims = Map.of(TokenProvider.CLAIM_FIRST_NAME, "John");
        
        String accessToken = tokenProvider.createAccessToken(subject, claims);
        String refreshToken = tokenProvider.createRefreshToken(subject, claims);
        
        Claims accessClaims = tokenProvider.getAllClaims(accessToken);
        Claims refreshClaims = tokenProvider.getAllClaims(refreshToken);
        
        assertTrue(refreshClaims.getExpiration().after(accessClaims.getExpiration()));
    }

    @Test
    void tokenConstants_AreCorrectlyDefined() {
        assertEquals("access_token", TokenProvider.TOKEN_TYPE_ACCESS);
        assertEquals("refresh_token", TokenProvider.TOKEN_TYPE_REFRESH);
        assertEquals("id_token", TokenProvider.TOKEN_TYPE_ID);
        assertEquals("email", TokenProvider.CLAIM_EMAIL);
        assertEquals("first_name", TokenProvider.CLAIM_FIRST_NAME);
        assertEquals("last_name", TokenProvider.CLAIM_LAST_NAME);
        assertEquals("user_uuid", TokenProvider.CLAIM_USER_ID);
        assertEquals("token_id", TokenProvider.CLAIM_TOKEN_ID);
        assertEquals("roles", TokenProvider.CLAIM_ROLES);
    }
    
    @Test
    void getAlgorithm_ReturnsConfiguredAlgorithm() {
        String algorithm = tokenProvider.getAlgorithm();
        assertEquals("HS256", algorithm);
    }
    
    @Test
    void getKeyId_ReturnsConfiguredKeyId() {
        String keyId = tokenProvider.getKeyId();
        assertEquals("test-key-1", keyId);
    }
}
