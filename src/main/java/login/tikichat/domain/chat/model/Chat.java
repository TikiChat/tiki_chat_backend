package login.tikichat.domain.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chats")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class Chat {
    @Id
    private String id;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, name = "sender_user_id")
    private Long senderUserId;

    @Column(nullable = false, name = "chat_room_id")
    private Long chatRoomId;

    @CreatedDate
    private Instant createdDate;

    public static Chat sendMessage(
            Long senderUserId,
            Long chatRoomId,
            String content
    ) {
        return Chat
                .builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .senderUserId(senderUserId)
                .build();
    }
}
