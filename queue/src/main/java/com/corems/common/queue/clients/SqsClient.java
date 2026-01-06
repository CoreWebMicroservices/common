package com.corems.common.queue.clients;

import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.config.QueueProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * AWS SQS implementation of QueueClient.
 * TODO: Implement when AWS SQS support is needed.
 */
@Slf4j
public class SqsClient implements QueueClient {
    
    private final QueueProperties.SqsProperties props;
    
    public SqsClient(QueueProperties.SqsProperties props) {
        this.props = props;
    }
    
    @Override
    public QueueProperties.SqsProperties getProperties() {
        return props;
    }
    
    @Override
    public void send(QueueMessage message) {
        throw new UnsupportedOperationException("SQS implementation not yet available");
    }
    
    @Override
    public void send(String destination, QueueMessage message) {
        throw new UnsupportedOperationException("SQS implementation not yet available");
    }
    
    @Override
    public Optional<QueueMessage> poll() {
        throw new UnsupportedOperationException("SQS implementation not yet available");
    }
    
    @Override
    public Optional<QueueMessage> poll(String destination) {
        throw new UnsupportedOperationException("SQS implementation not yet available");
    }
    
    @Override
    public List<QueueMessage> pollBatch(String destination, int maxMessages) {
        // SQS supports native batch operations - implement when needed
        throw new UnsupportedOperationException("SQS implementation not yet available");
    }
}