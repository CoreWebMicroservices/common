package com.corems.common.queue.util;

import com.corems.common.queue.QueueMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class QueueMDCUtilTest {

    @BeforeEach
    @AfterEach
    void clearMDC() {
        MDC.clear();
    }

    @Test
    void ensureCorrelationId_WhenMDCEmpty_CreatesNewCorrelationId() {
        String correlationId = QueueMDCUtil.ensureCorrelationId();
        
        assertNotNull(correlationId);
        assertEquals(correlationId, MDC.get(QueueMDCUtil.CORRELATION_ID_KEY));
    }

    @Test
    void ensureCorrelationId_WhenMDCHasCorrelationId_ReturnsExisting() {
        String existingCorrelationId = "existing-correlation-123";
        MDC.put(QueueMDCUtil.CORRELATION_ID_KEY, existingCorrelationId);
        
        String correlationId = QueueMDCUtil.ensureCorrelationId();
        
        assertEquals(existingCorrelationId, correlationId);
        assertEquals(existingCorrelationId, MDC.get(QueueMDCUtil.CORRELATION_ID_KEY));
    }

    @Test
    void setupProducerMDC_ReturnsCorrelationId() {
        String correlationId = QueueMDCUtil.ensureCorrelationId();
        
        assertNotNull(correlationId);
        assertEquals(correlationId, MDC.get(QueueMDCUtil.CORRELATION_ID_KEY));
    }

    @Test
    void setupConsumerMDC_PopulatesMDCFromMessage() {
        QueueMessage message = new QueueMessage();
        message.setCorrelationId("test-correlation");
        message.setType("TEST_MESSAGE");
        message.setSourceService("test-service");
        message.getHeaders().put(QueueMDCUtil.TRACE_ID_KEY, "trace-123");
        
        QueueMDCUtil.setupConsumerMDC(message);
        
        assertEquals("test-correlation", MDC.get(QueueMDCUtil.CORRELATION_ID_KEY));
        assertEquals("trace-123", MDC.get(QueueMDCUtil.TRACE_ID_KEY));
        assertEquals(message.getId(), MDC.get("messageId"));
        assertEquals("TEST_MESSAGE", MDC.get("messageType"));
        assertEquals("test-service", MDC.get("sourceService"));
    }

    @Test
    void cleanupConsumerMDC_RemovesMessageSpecificEntries() {
        // Setup MDC with both request-level and message-specific entries
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
    }

}