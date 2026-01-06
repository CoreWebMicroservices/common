package com.corems.common.queue;

import com.corems.common.queue.config.QueueClientProperties;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface QueueClient {
    QueueClientProperties getProperties();

    void send(QueueMessage message);
    void send(String destination, QueueMessage message);
    
    Optional<QueueMessage> poll();
    Optional<QueueMessage> poll(String destination);
    
    default void sendBatch(List<QueueMessage> messages) {
        messages.forEach(this::send);
    }
    
    default void sendBatch(String destination, List<QueueMessage> messages) {
        messages.forEach(msg -> send(destination, msg));
    }
    
    default List<QueueMessage> pollBatch(int maxMessages) {
        return pollBatch(getProperties().getDefaultQueue(), maxMessages);
    }
    
    default List<QueueMessage> pollBatch(String destination, int maxMessages) {
        return List.of(); // Default implementation - providers can optimize
    }
    
    default <T> CompletableFuture<Void> sendAsync(QueueMessage message) {
        return CompletableFuture.runAsync(() -> send(message));
    }
    
    default CompletableFuture<Optional<QueueMessage>> pollAsync(String destination) {
        return CompletableFuture.supplyAsync(() -> poll(destination));
    }
    
    default boolean isHealthy() {
        return true;
    }
}
