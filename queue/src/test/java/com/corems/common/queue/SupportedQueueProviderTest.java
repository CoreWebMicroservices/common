package com.corems.common.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupportedQueueProviderTest {

    @Test
    void fromString_WithValidRabbitMqVariants_ReturnsRabbitMq() {
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("rabbitmq"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("rabbit_mq"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("rabbit-mq"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("rabbit"));
    }

    @Test
    void fromString_WithValidSqsVariants_ReturnsSqs() {
        assertEquals(SupportedQueueProvider.AWS_SQS, SupportedQueueProvider.fromString("sqs"));
        assertEquals(SupportedQueueProvider.AWS_SQS, SupportedQueueProvider.fromString("aws_sqs"));
        assertEquals(SupportedQueueProvider.AWS_SQS, SupportedQueueProvider.fromString("aws-sqs"));
        assertEquals(SupportedQueueProvider.AWS_SQS, SupportedQueueProvider.fromString("awssqs"));
    }

    @Test
    void fromString_WithValidKafkaVariants_ReturnsKafka() {
        assertEquals(SupportedQueueProvider.APACHE_KAFKA, SupportedQueueProvider.fromString("kafka"));
        assertEquals(SupportedQueueProvider.APACHE_KAFKA, SupportedQueueProvider.fromString("apache_kafka"));
        assertEquals(SupportedQueueProvider.APACHE_KAFKA, SupportedQueueProvider.fromString("apache-kafka"));
    }

    @Test
    void fromString_WithCaseInsensitiveInput_ReturnsCorrectProvider() {
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("RABBITMQ"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("RabbitMQ"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("RABBIT_MQ"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("Rabbit-MQ"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("RABBIT"));
        
        assertEquals(SupportedQueueProvider.AWS_SQS, SupportedQueueProvider.fromString("SQS"));
        assertEquals(SupportedQueueProvider.AWS_SQS, SupportedQueueProvider.fromString("AWS_SQS"));
        
        assertEquals(SupportedQueueProvider.APACHE_KAFKA, SupportedQueueProvider.fromString("KAFKA"));
        assertEquals(SupportedQueueProvider.APACHE_KAFKA, SupportedQueueProvider.fromString("APACHE_KAFKA"));
    }

    @Test
    void fromString_WithWhitespace_HandlesCorrectly() {
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("  rabbitmq  "));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("\trabbit\t"));
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.fromString("\nrabbit_mq\n"));
    }

    @Test
    void fromString_WithInvalidInput_ReturnsNull() {
        assertNull(SupportedQueueProvider.fromString("activemq"));
        assertNull(SupportedQueueProvider.fromString("redis"));
        assertNull(SupportedQueueProvider.fromString("invalid"));
        assertNull(SupportedQueueProvider.fromString(""));
    }

    @Test
    void fromString_WithNullInput_ReturnsNull() {
        assertNull(SupportedQueueProvider.fromString(null));
    }

    @Test
    void fromString_WithEmptyString_ReturnsNull() {
        assertNull(SupportedQueueProvider.fromString(""));
        assertNull(SupportedQueueProvider.fromString("   "));
        assertNull(SupportedQueueProvider.fromString("\t"));
        assertNull(SupportedQueueProvider.fromString("\n"));
    }

    @Test
    void enumValues_ContainsExpectedProviders() {
        SupportedQueueProvider[] values = SupportedQueueProvider.values();
        assertEquals(3, values.length);
        assertEquals(SupportedQueueProvider.RABBIT_MQ, values[0]);
        assertEquals(SupportedQueueProvider.AWS_SQS, values[1]);
        assertEquals(SupportedQueueProvider.APACHE_KAFKA, values[2]);
    }

    @Test
    void enumName_IsCorrect() {
        assertEquals("RABBIT_MQ", SupportedQueueProvider.RABBIT_MQ.name());
    }

    @Test
    void valueOf_WorksCorrectly() {
        assertEquals(SupportedQueueProvider.RABBIT_MQ, SupportedQueueProvider.valueOf("RABBIT_MQ"));
    }

    @Test
    void valueOf_WithInvalidName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> SupportedQueueProvider.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> SupportedQueueProvider.valueOf("rabbitmq"));
        assertThrows(IllegalArgumentException.class, () -> SupportedQueueProvider.valueOf("KAFKA"));
    }

    @Test
    void fromString_EdgeCases() {
        // Test various edge cases that should return null
        assertNull(SupportedQueueProvider.fromString("rabbit_"));
        assertNull(SupportedQueueProvider.fromString("_rabbit"));
        assertNull(SupportedQueueProvider.fromString("rabbit__mq"));
        assertNull(SupportedQueueProvider.fromString("rabbit-"));
        assertNull(SupportedQueueProvider.fromString("-rabbit"));
        assertNull(SupportedQueueProvider.fromString("rabbit--mq"));
    }
}