package org.example.rec.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 推荐通知消息
 * 声明交换机、队列、绑定关系
 */
@Configuration
public class RabbitMQConfig {

    /** 推荐通知交换机 */
    public static final String EXCHANGE = "rec.exchange";
    /** 推荐通知队列 */
    public static final String QUEUE = "rec.notification.queue";
    /** 路由键 */
    public static final String ROUTING_KEY = "rec.notification.#";

    @Bean
    public TopicExchange recExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue recNotificationQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding recBinding() {
        return BindingBuilder
                .bind(recNotificationQueue())
                .to(recExchange())
                .with(ROUTING_KEY);
    }
}
