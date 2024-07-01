package login.tikichat.domain.chat.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
@Getter
public class SendChatEventDto {
    private final String id;
    private final String content;
    private final Long senderUserId;
    private final Long chatRoomId;
    private final Instant createdDate;
}
