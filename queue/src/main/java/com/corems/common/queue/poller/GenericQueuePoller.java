package com.corems.common.queue.poller;

import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.config.QueueProperties;
import com.corems.common.queue.util.QueueMDCUtil;
import com.corems.common.queue.util.QueueSecurityContextUtil;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class GenericQueuePoller implements AutoCloseable {

    private final QueueClient queueClient;
    private final Map<String, MessageHandler> handlers;
    private final String destination;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService pollExecutor;
    private final QueueProperties.RetryPolicy retryPolicy;

    public GenericQueuePoller(QueueClient queueClient, Map<String, MessageHandler> handlers) {
        this(queueClient, handlers, Collections.emptyMap(), true);
    }

    public GenericQueuePoller(QueueClient queueClient, Map<String, MessageHandler> handlers, Map<String, String> defaultDestination) {
        this(queueClient, handlers, defaultDestination, true);
    }

    public GenericQueuePoller(QueueClient queueClient, Map<String, MessageHandler> handlers, Map<String, String> defaultDestination, boolean autoStart) {
        this.queueClient = queueClient;
        this.handlers = handlers;
        this.destination = queueClient.getProperties().getDefaultQueue();
        this.retryPolicy = getRetryPolicy(queueClient);
        this.pollExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "generic-queue-poller");
            t.setDaemon(true);
            return t;
        });

        if (autoStart) start();
    }

    private QueueProperties.RetryPolicy getRetryPolicy(QueueClient queueClient) {
        if (queueClient.getProperties() instanceof QueueProperties.RabbitMqProperties rabbitProps) {
            return rabbitProps.getRetryPolicy();
        }
        // Default retry policy
        QueueProperties.RetryPolicy defaultPolicy = new QueueProperties.RetryPolicy();
        defaultPolicy.setMaxAttempts(queueClient.getProperties().getRetryCount());
        return defaultPolicy;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            pollExecutor.submit(this::runLoop);
            log.info("GenericQueuePoller started for destination={}", destination);
        }
    }

    private void runLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnce();
            } catch (Exception ex) {
                log.error("Unexpected error in GenericQueuePoller loop", ex);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("GenericQueuePoller loop exiting for destination={}", destination);
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                pollExecutor.shutdownNow();
                if (!pollExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("GenericQueuePoller executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("GenericQueuePoller stopped for destination={}", destination);
        }
    }

    @Override
    public void close() {
        stop();
    }

    public void pollOnce() {
        try {
            Optional<QueueMessage> maybe = queueClient.poll(destination);
            if (maybe.isEmpty()) {
                return;
            }

            QueueMessage qm = maybe.get();
            
            // Check if message is expired
            if (qm.isExpired()) {
                log.warn("Discarding expired message id={}", qm.getId());
                return;
            }
            
            processMessage(qm);
        } catch (Exception e) {
            log.error("Unexpected error while polling queue", e);
        }
    }

    protected void processMessage(QueueMessage qm) {
        QueueMDCUtil.setupConsumerMDC(qm);
        
        try {
            QueueSecurityContextUtil.setSecurityContextFromQueueUser(qm.getUser());
            
            MessageHandler handler = handlers.get(qm.getType());
            if (handler == null) {
                log.error("No handler registered for message type={}", qm.getType());
                return;
            }
            
            qm.incrementAttempts();
            handler.handle(qm);
            
        } catch (Exception ex) {
            log.error("Failed handling message id={}: {}", qm.getId(), ex.getMessage());
            
            if (shouldRetry(qm, ex)) {
                scheduleRetry(qm);
            } else {
                handleFailedMessage(qm, ex);
            }
        } finally {
            QueueSecurityContextUtil.clearSecurityContext();
            QueueMDCUtil.cleanupConsumerMDC();
        }
    }

    private boolean shouldRetry(QueueMessage qm, Exception ex) {
        if (qm.getAttempts() >= retryPolicy.getMaxAttempts()) {
            return false;
        }
        
        // Check if exception is non-retryable
        if (!retryPolicy.getNonRetryableExceptions().isEmpty()) {
            return retryPolicy.getNonRetryableExceptions().stream()
                    .noneMatch(exClass -> exClass.isInstance(ex));
        }
        
        // Check if exception is explicitly retryable
        if (!retryPolicy.getRetryableExceptions().isEmpty()) {
            return retryPolicy.getRetryableExceptions().stream()
                    .anyMatch(exClass -> exClass.isInstance(ex));
        }
        
        return true; // Default to retryable
    }

    private void scheduleRetry(QueueMessage qm) {
        try {
            // Calculate backoff delay
            long backoffMs = calculateBackoff(qm.getAttempts());
            
            if (backoffMs > 0) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            queueClient.send(destination, qm);
            log.info("Re-enqueued message id={} for retry attempt {} (destination={})", 
                    qm.getId(), qm.getAttempts(), destination);
        } catch (Exception e) {
            log.error("Failed to re-enqueue message id={}", qm.getId(), e);
            handleFailedMessage(qm, e);
        }
    }

    private long calculateBackoff(int attempt) {
        Duration initialBackoff = retryPolicy.getInitialBackoff();
        double multiplier = retryPolicy.getBackoffMultiplier();
        Duration maxBackoff = retryPolicy.getMaxBackoff();
        
        long backoffMs = (long) (initialBackoff.toMillis() * Math.pow(multiplier, attempt - 1));
        return Math.min(backoffMs, maxBackoff.toMillis());
    }

    private void handleFailedMessage(QueueMessage qm, Exception ex) {
        log.warn("Message id={} exhausted {} attempts, handling final failure", qm.getId(), qm.getAttempts());
        
        // Send to dead letter queue if configured
        if (retryPolicy.getDeadLetterQueue() != null && !retryPolicy.getDeadLetterQueue().isBlank()) {
            try {
                qm.withHeader("failure-reason", ex.getMessage())
                  .withHeader("failed-at", Instant.now().toString());
                queueClient.send(retryPolicy.getDeadLetterQueue(), qm);
                log.info("Sent failed message id={} to dead letter queue: {}", qm.getId(), retryPolicy.getDeadLetterQueue());
            } catch (Exception dlqEx) {
                log.error("Failed to send message id={} to dead letter queue", qm.getId(), dlqEx);
            }
        }
        
        // Final handler call for cleanup
        try {
            MessageHandler handler = handlers.get(qm.getType());
            if (handler != null) {
                handler.handle(qm);
            }
        } catch (Exception finalEx) {
            log.info("Handler processed final failure for message id={}", qm.getId());
        }
    }
}
