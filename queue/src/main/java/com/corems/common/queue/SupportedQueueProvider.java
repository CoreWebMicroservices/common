package com.corems.common.queue;

/**
 * Enum of supported queue providers.
 */
public enum SupportedQueueProvider {
    RABBIT_MQ,
    AWS_SQS,
    APACHE_KAFKA;

    public static SupportedQueueProvider fromString(String s) {
        if (s == null) return null;
        String v = s.trim().toLowerCase();
        return switch (v) {
            case "rabbitmq", "rabbit_mq", "rabbit-mq", "rabbit" -> RABBIT_MQ;
            case "sqs", "aws_sqs", "aws-sqs", "awssqs" -> AWS_SQS;
            case "kafka", "apache_kafka", "apache-kafka" -> APACHE_KAFKA;
            default -> null;
        };
    }
}
