package login.tikichat.domain.chat.pubsub.impl;

import login.tikichat.domain.chat.pubsub.SendChatProducer;
import login.tikichat.domain.chat.dto.SendChatEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendChatProducerImpl implements SendChatProducer {
    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic chatChannelTopic;

    @Override
    public void sendMessage(SendChatEventDto sendChatEventDto) {
        this.redisTemplate.convertAndSend(chatChannelTopic.getTopic(), sendChatEventDto);
    }
}
