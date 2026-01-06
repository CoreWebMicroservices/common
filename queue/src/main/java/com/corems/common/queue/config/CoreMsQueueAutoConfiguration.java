package com.corems.common.queue.config;

import com.corems.common.queue.QueueProvider;
import com.corems.common.queue.clients.RabbitMqClient;
import com.corems.common.queue.SupportedQueueProvider;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(QueueProperties.class)
public class CoreMsQueueAutoConfiguration {

    @Bean
    public MessageConverter coremsJacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public QueueProvider queueProvider(QueueProperties props, MessageConverter converter) {
        QueueProvider provider = new QueueProvider(props);

        QueueProperties.RabbitMqProperties rabbitProps = props.getProviders().getRabbitMq();
        if (props.isEnabled() && rabbitProps != null) {
            provider.registerProvider(
                    SupportedQueueProvider.RABBIT_MQ,
                    RabbitMqClient.createRabbitMqClient(rabbitProps, converter));
        }

        return provider;
    }
}
