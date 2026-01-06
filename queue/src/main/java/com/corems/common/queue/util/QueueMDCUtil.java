package com.corems.common.queue.util;

import com.corems.common.queue.QueueMessage;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing MDC (Mapped Diagnostic Context) in queue operations.
 * Provides consistent correlation ID and tracing context management across producers and consumers.
 */
public final class QueueMDCUtil {

    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";

    private QueueMDCUtil() {
        // Utility class
    }
    
    /**
     * Ensures a correlation ID exists in MDC, creating one if necessary.
     * @return the correlation ID that was set or already existed
     */
    public static String ensureCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
        return correlationId;
    }
    
    /**
     * Prepares a message for sending by ensuring it has correlation ID and tracing context.
     * If the message doesn't have a correlation ID, it will be populated from MDC or auto-generated.
     * Tracing context from MDC will be captured in message headers.
     * 
     * @param message the message to prepare
     * @param <T> the message payload type
     * @return the same message instance for fluent usage
     */
    public static QueueMessage prepareMessageForSending(QueueMessage message) {
        // Set correlation ID if not already set by user
        if (message.getCorrelationId() == null) {
            String correlationId = MDC.get(CORRELATION_ID_KEY);
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
                MDC.put(CORRELATION_ID_KEY, correlationId);
            }
            message.setCorrelationId(correlationId);
        }
        
        // Capture tracing context from MDC into message headers (only if not already set)
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null && !message.getHeaders().containsKey(TRACE_ID_KEY)) {
            message.getHeaders().put(TRACE_ID_KEY, traceId);
        }
        
        String spanId = MDC.get(SPAN_ID_KEY);
        if (spanId != null && !message.getHeaders().containsKey(SPAN_ID_KEY)) {
            message.getHeaders().put(SPAN_ID_KEY, spanId);
        }
        
        return message;
    }
    
    /**
     * Sets up MDC context for message processing.
     * This should be called by consumers before processing a message.
     * 
     * @param message the message being processed
     */
    public static void setupConsumerMDC(QueueMessage message) {
        if (message.getCorrelationId() != null) {
            MDC.put(CORRELATION_ID_KEY, message.getCorrelationId());
        }
        
        // Restore tracing context from headers
        String traceId = message.getHeaders().get(TRACE_ID_KEY);
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
        
        String spanId = message.getHeaders().get(SPAN_ID_KEY);
        if (spanId != null) {
            MDC.put(SPAN_ID_KEY, spanId);
        }
        
        // Add message-specific context
        MDC.put("messageId", message.getId());
        MDC.put("messageType", message.getType());
        if (message.getSourceService() != null) {
            MDC.put("sourceService", message.getSourceService());
        }
    }
    
    /**
     * Cleans up message-specific MDC entries after processing.
     * This preserves request-level correlation IDs while removing message-specific context.
     */
    public static void cleanupConsumerMDC() {
        MDC.remove("messageId");
        MDC.remove("messageType");
        MDC.remove("sourceService");
    }

    
}