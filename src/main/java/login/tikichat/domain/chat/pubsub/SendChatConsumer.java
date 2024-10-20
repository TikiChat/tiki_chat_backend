package login.tikichat.domain.chat.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import login.tikichat.domain.chat.dto.FindChatsDto;
import login.tikichat.domain.chat.dto.SendChatEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendChatConsumer implements MessageListener {
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void onMessage(@Nonnull Message message, byte[] pattern) {
        try {
            String publishMessage = redisTemplate.getStringSerializer().deserialize(message.getBody());
            SendChatEventDto sendChatEventDto = objectMapper.readValue(publishMessage, SendChatEventDto.class);

            FindChatsDto.FindChatsItemRes findChatsItemRes = new FindChatsDto.FindChatsItemRes(
                    sendChatEventDto.getId(),
                    sendChatEventDto.getContent(),
                    sendChatEventDto.getCreatedDate(),
                    Collections.emptyList(),
                    sendChatEventDto.getParent() != null ? new FindChatsDto.FindChatsParentItemRes(
                            sendChatEventDto.getParent().getId(),
                            sendChatEventDto.getParent().getContent(),
                            sendChatEventDto.getParent().getCreatedDate()
                    ) : null,
                    sendChatEventDto.getImageUrls()
            );

            simpMessagingTemplate.convertAndSend(
                    "/sub/chat-rooms/" + sendChatEventDto.getChatRoomId() + "/chats",
                    findChatsItemRes
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
