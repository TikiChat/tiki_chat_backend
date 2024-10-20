package login.tikichat.domain.chat.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import login.tikichat.domain.attachment.model.ChatAttachment;
import login.tikichat.domain.attachment.model.ChatRoomAttachment;
import login.tikichat.domain.chatroom.model.ChatRoom;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chats",
        indexes = {
                @Index(columnList = "parent_chat_id")
        }
)
@AllArgsConstructor
@Builder(access = AccessLevel.PROTECTED)
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, name = "sender_user_id")
    private Long senderUserId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @CreatedDate
    private Instant createdDate;

    @Builder.Default
    @OneToMany(mappedBy = "chat")
    private Set<ChatReaction> chatReactions = new HashSet<>();

    @Column(name = "parent_chat_id")
    private Long parentChatId;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatAttachment> attachments = new ArrayList<>();

    public static Chat sendMessage(
            Long senderUserId,
            ChatRoom chatRoom,
            String content,
            Long parentChatId
    ) {
        return Chat
                .builder()
                .chatRoom(chatRoom)
                .content(content)
                .senderUserId(senderUserId)
                .parentChatId(parentChatId)
                .createdDate(Instant.now())
                .attachments(new ArrayList<>())
                .build();
    }
}
