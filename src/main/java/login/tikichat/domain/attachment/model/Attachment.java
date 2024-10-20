package login.tikichat.domain.attachment.model;

import jakarta.persistence.*;
import login.tikichat.domain.chatroom.model.ChatRoom;
import login.tikichat.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@Table(name = "attachments")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(length = 4000, nullable = false)
    protected String path;

    @Column(length = 1000, nullable = false)
    protected String filename;

    @Column(length = 1000, nullable = false)
    protected String originalFilename;

    @Column(nullable = false)
    protected String ext;

    @ManyToOne()
    @JoinColumn(name = "uploader_user_id", nullable = false)
    protected User uploaderUser;
}
