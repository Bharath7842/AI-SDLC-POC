package com.portpoc.donor.infrastructure.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PORT_EXCHANGE = "port-exchange";
    public static final String PORT_REQUEST_INITIATED_QUEUE = "port.request.initiated";
    public static final String PORT_REQUEST_INITIATED_ROUTING_KEY = "port.request.initiated";
    public static final String PORT_REQUEST_ACCEPTED_QUEUE = "port.request.accepted";
    public static final String PORT_REQUEST_ACCEPTED_ROUTING_KEY = "port.request.accepted";
    public static final String PORT_DLX = "port-dlx";

    @Bean
    public DirectExchange portExchange() {
        return new DirectExchange(PORT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange portDlx() {
        return new DirectExchange(PORT_DLX, true, false);
    }

    @Bean
    public Queue portRequestInitiatedQueue() {
        return new Queue(PORT_REQUEST_INITIATED_QUEUE, true, false, false,
            org.springframework.amqp.core.QueueBuilder.durable(PORT_REQUEST_INITIATED_QUEUE)
                .withArgument("x-dead-letter-exchange", PORT_DLX)
                .withArgument("x-dead-letter-routing-key", PORT_REQUEST_INITIATED_QUEUE + ".dlq")
                .build().getArguments()
        );
    }

    @Bean
    public Queue portRequestAcceptedQueue() {
        return new Queue(PORT_REQUEST_ACCEPTED_QUEUE, true, false, false,
            org.springframework.amqp.core.QueueBuilder.durable(PORT_REQUEST_ACCEPTED_QUEUE)
                .withArgument("x-dead-letter-exchange", PORT_DLX)
                .withArgument("x-dead-letter-routing-key", PORT_REQUEST_ACCEPTED_QUEUE + ".dlq")
                .build().getArguments()
        );
    }

    @Bean
    public Queue portRequestInitiatedDlq() {
        return new Queue(PORT_REQUEST_INITIATED_QUEUE + ".dlq", true);
    }

    @Bean
    public Queue portRequestAcceptedDlq() {
        return new Queue(PORT_REQUEST_ACCEPTED_QUEUE + ".dlq", true);
    }

    @Bean
    public Binding bindPortRequestInitiatedQueue(Queue portRequestInitiatedQueue, DirectExchange portExchange) {
        return BindingBuilder.bind(portRequestInitiatedQueue)
            .to(portExchange)
            .with(PORT_REQUEST_INITIATED_ROUTING_KEY);
    }

    @Bean
    public Binding bindPortRequestAcceptedQueue(Queue portRequestAcceptedQueue, DirectExchange portExchange) {
        return BindingBuilder.bind(portRequestAcceptedQueue)
            .to(portExchange)
            .with(PORT_REQUEST_ACCEPTED_ROUTING_KEY);
    }

    @Bean
    public Binding bindInitiatedDlq(Queue portRequestInitiatedDlq, DirectExchange portDlx) {
        return BindingBuilder.bind(portRequestInitiatedDlq)
            .to(portDlx)
            .with(PORT_REQUEST_INITIATED_QUEUE + ".dlq");
    }

    @Bean
    public Binding bindAcceptedDlq(Queue portRequestAcceptedDlq, DirectExchange portDlx) {
        return BindingBuilder.bind(portRequestAcceptedDlq)
            .to(portDlx)
            .with(PORT_REQUEST_ACCEPTED_QUEUE + ".dlq");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        return factory;
    }
}
