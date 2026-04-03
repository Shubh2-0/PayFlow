package com.payflow.wallet.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "payflow.exchange";
    public static final String TRANSACTION_QUEUE = "payflow.transaction.queue";
    public static final String NOTIFICATION_QUEUE = "payflow.notification.queue";
    public static final String TRANSACTION_ROUTING_KEY = "transaction.completed";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";

    @Bean
    public TopicExchange payflowExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue transactionQueue() {
        return QueueBuilder.durable(TRANSACTION_QUEUE).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding transactionBinding(Queue transactionQueue, TopicExchange payflowExchange) {
        return BindingBuilder.bind(transactionQueue).to(payflowExchange).with(TRANSACTION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange payflowExchange) {
        return BindingBuilder.bind(notificationQueue).to(payflowExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
