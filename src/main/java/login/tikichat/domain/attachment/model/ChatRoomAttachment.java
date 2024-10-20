package login.tikichat.domain.attachment.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import login.tikichat.domain.chatroom.model.ChatRoom;
import login.tikichat.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("chat_room")
@Table(name = "chat_room_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomAttachment extends Attachment {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    public ChatRoomAttachment(
            User uploaderUser,
            ChatRoom chatRoom,
            String path,
            String filename,
            String originalFilename,
            String ext
    ) {
        this.filename = filename;
        this.uploaderUser = uploaderUser;
        this.chatRoom = chatRoom;
        this.path = path;
        this.originalFilename = originalFilename;
        this.ext = ext;
    }

    // ChatRoom 과의 연관관계 설정 편의 메서드
    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;

        if (chatRoom != null && !chatRoom.getAttachments().contains(this)) {
            chatRoom.getAttachments().add(this);
        }
    }
}
