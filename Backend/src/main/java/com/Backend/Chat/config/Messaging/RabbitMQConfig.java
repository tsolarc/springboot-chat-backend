package com.Backend.Chat.config.Messaging;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Bean
    public Queue defaultChatQueue() {
        return QueueBuilder.durable("chat.default.queue")
                .withArgument("x-dead-letter-exchange", "chat.dlx")
                .withArgument("x-dead-letter-routing-key", "chat.dead")
                .build();
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange("chat.exchange", true, false);
    }

    @Bean
    public Binding defaultChatBinding() {
        return BindingBuilder.bind(defaultChatQueue())
                .to(chatExchange())
                .with("chat.default");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("chat.dead.queue").build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("chat.dlx");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("chat.dead");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack == false) {
                log.error("Message was not confirmed: {}", cause);
            }
        });
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(RabbitTemplate rabbitTemplate) {
        return new RabbitAdmin(rabbitTemplate);
    }
}
