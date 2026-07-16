package org.example.rec.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 推荐通知生产者 — 定时任务完成后发送消息
 * 消息会被其他服务（如 module-chat）消费，用于通知用户
 */
@Component
public class RecNotificationProducer {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送"今日推荐已更新"通知
     * @param userId    用户ID
     * @param songCount 推荐的歌曲数量
     */
    public void sendDailyReady(Integer userId, int songCount) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("userId", userId);
        msg.put("type", "DAILY_RECOMMEND_READY");
        msg.put("message", "今日推荐已更新，为你准备了 " + songCount + " 首歌 🎵");
        msg.put("timestamp", LocalDateTime.now().toString());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                "rec.notification.daily",
                msg
        );
    }
}
