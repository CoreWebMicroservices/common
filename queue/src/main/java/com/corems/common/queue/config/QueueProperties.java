package com.corems.common.queue.config;

import com.corems.common.queue.SupportedQueueProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "queue")
@Validated
@Setter
@Getter
public class QueueProperties {

    private boolean enabled = false;
    private SupportedQueueProvider provider = SupportedQueueProvider.RABBIT_MQ;
    private Providers providers = new Providers();
    
    // Global queue settings that apply to all providers
    private GlobalSettings global = new GlobalSettings();

    @Getter
    @Setter
    public static class GlobalSettings {
        private int concurrency = 1;
        private int maxConcurrency = 5;
        private boolean enableMetrics = true;
        private Duration healthCheckInterval = Duration.ofMinutes(1);
    }

    @Getter
    @Setter
    public static class Providers {
        private RabbitMqProperties rabbitMq = new RabbitMqProperties();
        private SqsProperties sqs = new SqsProperties();
        private KafkaProperties kafka = new KafkaProperties();
    }

    @Getter
    @Setter
    public static class RabbitMqProperties implements QueueClientProperties {
        private String host = "localhost";
        private int port = 5672;
        private String username = "";
        private String password = "";
        private String exchange = "";
        private String defaultQueue = "";
        private List<String> requiredQueues = new ArrayList<>();
        private long pollIntervalMs = 1000L;
        private int retryCount = 3;
        
        // RabbitMQ-specific optimizations
        private int prefetchCount = 1;
        private boolean durableQueues = true;
        private boolean autoDeleteQueues = false;
        private RetryPolicy retryPolicy = new RetryPolicy();
        private Map<String, Object> connectionProperties = new HashMap<>();
    }

    @Getter
    @Setter
    public static class SqsProperties implements QueueClientProperties {
        private String region = "us-east-1";
        private String accessKey = "";
        private String secretKey = "";
        private String defaultQueue = "";
        private List<String> requiredQueues = new ArrayList<>();
        private long pollIntervalMs = 5000L; // SQS long polling
        private int retryCount = 3;
        
        // SQS-specific settings
        private int maxMessages = 10; // Batch size
        private int visibilityTimeoutSeconds = 30;
        private int waitTimeSeconds = 20; // Long polling
        private RetryPolicy retryPolicy = new RetryPolicy();
    }

    @Getter
    @Setter
    public static class KafkaProperties implements QueueClientProperties {
        private String bootstrapServers = "localhost:9092";
        private String defaultQueue = ""; // Topic name
        private List<String> requiredQueues = new ArrayList<>();
        private long pollIntervalMs = 1000L;
        private int retryCount = 3;
        
        // Kafka-specific settings
        private String consumerGroup = "corems-consumer";
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "org.apache.kafka.common.serialization.JsonSerializer";
        private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
        private String valueDeserializer = "org.apache.kafka.common.serialization.JsonDeserializer";
        private RetryPolicy retryPolicy = new RetryPolicy();
        private Map<String, Object> consumerProperties = new HashMap<>();
        private Map<String, Object> producerProperties = new HashMap<>();
    }

    @Getter
    @Setter
    public static class RetryPolicy {
        private int maxAttempts = 3;
        private Duration initialBackoff = Duration.ofSeconds(1);
        private double backoffMultiplier = 2.0;
        private Duration maxBackoff = Duration.ofSeconds(30);
        private String deadLetterQueue;
        private boolean useProviderNativeRetry = true;
        private List<Class<? extends Exception>> retryableExceptions = new ArrayList<>();
        private List<Class<? extends Exception>> nonRetryableExceptions = new ArrayList<>();
    }
}
