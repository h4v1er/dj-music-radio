package org.example.music.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 使用 JSON 序列化替代默认 Java 序列化
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "music.exchange";
    public static final String QUEUE_IMPORT = "music.playlist.import";
    public static final String ROUTING_KEY_IMPORT = "playlist.import";

    /** JSON 消息转换器 — 解决 HashMap 反序列化安全限制 */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** 配置 RabbitTemplate 使用 JSON */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    /** 配置 Listener 容器工厂使用 JSON */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }

    /** 声明交换机 */
    @Bean
    public DirectExchange musicExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    /** 声明歌单导入队列 */
    @Bean
    public Queue importQueue() {
        return QueueBuilder.durable(QUEUE_IMPORT).build();
    }

    /** 绑定导入队列到交换机 */
    @Bean
    public Binding importBinding() {
        return BindingBuilder.bind(importQueue()).to(musicExchange()).with(ROUTING_KEY_IMPORT);
    }
}
