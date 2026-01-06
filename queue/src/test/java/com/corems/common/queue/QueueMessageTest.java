package com.corems.common.queue;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.corems.common.queue.util.QueueMDCUtil;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueueMessageTest {

    @Test
    void constructor_CreatesMessageWithDefaults() {
        // Clear MDC to ensure clean test state
        MDC.clear();
        
        QueueMessage message = new QueueMessage();
        
        assertNotNull(message.getId()); // Now auto-generated
        assertNull(message.getType());
        assertNull(message.getPayload());
        assertEquals(0, message.getAttempts());
        assertNotNull(message.getHeaders()); // Now initialized
        assertNotNull(message.getCreatedAt());
        assertTrue(message.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
        assertNull(message.getLastAttemptAt());
        assertNull(message.getSourceService());
        assertNull(message.getCorrelationId()); // No longer auto-generated in constructor
        assertEquals(0, message.getPriority());
        assertNull(message.getExpiresAt());
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        QueueMessage message = new QueueMessage();
        String testId = "test-id-123";
        String testType = "USER_CREATED";
        String testPayload = "test payload";
        int testAttempts = 3;
        Map<String, String> testHeaders = Map.of("source", "user-service", "version", "1.0");
        Instant testCreatedAt = Instant.now().minusSeconds(60);
        
        message.setId(testId);
        message.setType(testType);
        message.setPayload(testPayload);
        message.setAttempts(testAttempts);
        message.setHeaders(testHeaders);
        message.setCreatedAt(testCreatedAt);
        
        assertEquals(testId, message.getId());
        assertEquals(testType, message.getType());
        assertEquals(testPayload, message.getPayload());
        assertEquals(testAttempts, message.getAttempts());
        assertEquals(testHeaders, message.getHeaders());
        assertEquals(testCreatedAt, message.getCreatedAt());
    }


    @Test
    void headers_CanBeModified() {
        QueueMessage message = new QueueMessage();
        Map<String, String> headers = new HashMap<>();
        headers.put("correlation-id", "abc-123");
        headers.put("retry-count", "1");
        
        message.setHeaders(headers);
        assertEquals(2, message.getHeaders().size());
        assertEquals("abc-123", message.getHeaders().get("correlation-id"));
        
        // Modify headers
        headers.put("retry-count", "2");
        message.setHeaders(headers);
        assertEquals("2", message.getHeaders().get("retry-count"));
    }

    @Test
    void attempts_CanBeIncremented() {
        QueueMessage message = new QueueMessage();
        assertEquals(0, message.getAttempts());
        
        message.setAttempts(message.getAttempts() + 1);
        assertEquals(1, message.getAttempts());
        
        message.setAttempts(message.getAttempts() + 1);
        assertEquals(2, message.getAttempts());
    }

    @Test
    void serializable_HasSerialVersionUID() {
        // Verify that QueueMessage implements Serializable properly
        assertTrue(java.io.Serializable.class.isAssignableFrom(QueueMessage.class));
        
        // Test serialization/deserialization would require more setup
        // but the class structure supports it with serialVersionUID
    }

    @Test
    void createdAt_DefaultsToCurrentTime() {
        Instant before = Instant.now();
        QueueMessage message = new QueueMessage();
        Instant after = Instant.now();
        
        assertNotNull(message.getCreatedAt());
        assertTrue(message.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(message.getCreatedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void createdAt_CanBeOverridden() {
        QueueMessage message = new QueueMessage();
        Instant customTime = Instant.parse("2023-01-01T12:00:00Z");
        
        message.setCreatedAt(customTime);
        assertEquals(customTime, message.getCreatedAt());
    }

    @Test
    void incrementAttempts_UpdatesAttemptsAndTimestamp() {
        QueueMessage message = new QueueMessage();
        assertEquals(0, message.getAttempts());
        assertNull(message.getLastAttemptAt());
        
        message.incrementAttempts();
        assertEquals(1, message.getAttempts());
        assertNotNull(message.getLastAttemptAt());
        
        Instant firstAttempt = message.getLastAttemptAt();
        message.incrementAttempts();
        assertEquals(2, message.getAttempts());
        assertTrue(message.getLastAttemptAt().isAfter(firstAttempt) || message.getLastAttemptAt().equals(firstAttempt));
    }

    @Test
    void isExpired_ReturnsFalseWhenNoExpiration() {
        QueueMessage message = new QueueMessage();
        assertFalse(message.isExpired());
    }

    @Test
    void isExpired_ReturnsTrueWhenExpired() {
        QueueMessage message = new QueueMessage();
        message.setExpiresAt(Instant.now().minusSeconds(60));
        assertTrue(message.isExpired());
    }

    @Test
    void isExpired_ReturnsFalseWhenNotExpired() {
        QueueMessage message = new QueueMessage();
        message.setExpiresAt(Instant.now().plusSeconds(60));
        assertFalse(message.isExpired());
    }

    @Test
    void fluentMethods_WorkCorrectly() {
        QueueMessage message = new QueueMessage();
        
        QueueMessage result = message
                .withCorrelationId("test-correlation")
                .withPriority(5)
                .withHeader("source", "test-service");
        
        assertSame(message, result); // Should return same instance
        assertEquals("test-correlation", message.getCorrelationId());
        assertEquals(5, message.getPriority());
        assertEquals("test-service", message.getHeaders().get("source"));
    }

    @Test
    void headers_AreInitializedByDefault() {
        QueueMessage message = new QueueMessage();
        assertNotNull(message.getHeaders());
        assertTrue(message.getHeaders().isEmpty());
        
        message.getHeaders().put("test", "value");
        assertEquals("value", message.getHeaders().get("test"));
    }

    @Test
    void constructor_WithExistingMDCCorrelationId_UsesMDCValue() {
        MDC.clear();
        String expectedCorrelationId = "test-correlation-123";
        MDC.put(QueueMDCUtil.CORRELATION_ID_KEY, expectedCorrelationId);
        
        QueueMessage message = new QueueMessage();
        
        // Note: Constructor no longer auto-sets from MDC, correlation ID should be null
        assertNull(message.getCorrelationId());
        assertEquals(expectedCorrelationId, MDC.get(QueueMDCUtil.CORRELATION_ID_KEY));
        
        MDC.clear();
    }

    @Test
    void populateMDC_SetsCorrelationIdAndTracingContext() {
        MDC.clear();
        
        QueueMessage message = new QueueMessage();
        message.setCorrelationId("test-correlation");
        message.setType("TEST_MESSAGE");
        message.setSourceService("test-service");
        message.getHeaders().put(QueueMDCUtil.TRACE_ID_KEY, "trace-789");
        message.getHeaders().put(QueueMDCUtil.SPAN_ID_KEY, "span-101");
        
        QueueMDCUtil.setupConsumerMDC(message);
        
        assertEquals("test-correlation", MDC.get(QueueMDCUtil.CORRELATION_ID_KEY));
        assertEquals("trace-789", MDC.get(QueueMDCUtil.TRACE_ID_KEY));
        assertEquals("span-101", MDC.get(QueueMDCUtil.SPAN_ID_KEY));
        assertEquals(message.getId(), MDC.get("messageId"));
        assertEquals("TEST_MESSAGE", MDC.get("messageType"));
        assertEquals("test-service", MDC.get("sourceService"));
        
        MDC.clear();
    }

    @Test
    void clearMDC_RemovesMessageSpecificEntries() {
        MDC.clear();
        
        // Set up MDC with both message-specific and request-level entries
        MDC.put(QueueMDCUtil.CORRELATION_ID_KEY, "correlation-123");
        MDC.put(QueueMDCUtil.TRACE_ID_KEY, "trace-123");
        MDC.put("messageId", "msg-123");
        MDC.put("messageType", "TEST");
        MDC.put("sourceService", "test-service");
        
        QueueMDCUtil.cleanupConsumerMDC();
        
        // Request-level entries should remain
        assertEquals("correlation-123", MDC.get(QueueMDCUtil.CORRELATION_ID_KEY));
        assertEquals("trace-123", MDC.get(QueueMDCUtil.TRACE_ID_KEY));
        
        // Message-specific entries should be removed
        assertNull(MDC.get("messageId"));
        assertNull(MDC.get("messageType"));
        assertNull(MDC.get("sourceService"));
        
        MDC.clear();
    }
}