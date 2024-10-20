package login.tikichat.domain.attachment.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import login.tikichat.domain.chat.model.Chat;
import login.tikichat.domain.chatroom.model.ChatRoom;
import login.tikichat.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("chat")
@Table(name = "chat_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAttachment extends Attachment {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    public ChatAttachment(
            User uploaderUser,
            Chat chat,
            String path,
            String filename,
            String originalFilename,
            String ext
    ) {
        this.filename = filename;
        this.uploaderUser = uploaderUser;
        this.chat = chat;
        this.path = path;
        this.originalFilename = originalFilename;
        this.ext = ext;

        chat.getAttachments().add(this);
    }
}
