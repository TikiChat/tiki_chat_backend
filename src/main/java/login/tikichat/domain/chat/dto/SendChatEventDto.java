package login.tikichat.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SendChatEventDto {
    private Long id;
    private String content;
    private Long senderUserId;
    private Long chatRoomId;
    private Instant createdDate;
    private SendChatParentDto parent;
    private List<String> imageUrls;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendChatParentDto {
        private Long id;
        private String content;
        private Long senderUserId;
        private Instant createdDate;
    }
}
