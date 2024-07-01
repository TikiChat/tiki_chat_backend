package login.tikichat.domain.chat.pubsub;

import login.tikichat.domain.chat.dto.SendChatEventDto;

public interface SendChatProducer {
    void sendMessage(SendChatEventDto sendChatEventDto);
}
