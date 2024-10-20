package login.tikichat.domain.chatroom.model;

import jakarta.persistence.*;
import login.tikichat.domain.attachment.model.Attachment;
import login.tikichat.domain.attachment.model.ChatRoomAttachment;
import login.tikichat.domain.category.model.Category;
import login.tikichat.domain.chatroom_participant.model.ChatRoomParticipant;
import login.tikichat.domain.host.model.Host;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_user_count", nullable = false)
    private Integer maxUserCount;

    @Column(name = "current_user_count", nullable = false)
    private Integer currentUserCount;

    @Column(name = "image_url", nullable = false)
    private URL imageUrl; // 프로필 이미지

    @Column(name = "tags", columnDefinition = "json", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> tags;

    // Host: 채팅방 개설자
    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_code", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<ChatRoomParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<ChatRoomAttachment> attachments = new ArrayList<>();

    private boolean isRoomClosed;

    public ChatRoom(
            Host host,
            String name,
            Integer maxUserCount,
            URL imageUrl,
            List<String> tags,
            Category category
    ) {
        this.host = host;
        this.name = name;
        this.maxUserCount = maxUserCount;
        this.imageUrl = imageUrl;
        this.tags = tags;
        this.category = category;
        this.currentUserCount = 0;
        this.isRoomClosed = false;
    }

    public void close() {
        this.isRoomClosed = true;
        this.currentUserCount = 0;
    }

    // 연관관계 편의 메소드
    public void addAttachment(ChatRoomAttachment attachment) {
        if (!this.attachments.contains(attachment)) {
            this.attachments.add(attachment);
            attachment.setChatRoom(this);
        }
    }

    // Host와의 연관관계 설정 편의 메서드
    public void setHost(Host host) {
        // 새로운 User 설정
        this.host = host;

        // 새로운 Host 의 ChatRoom 리스트에 현재 ChatRoom 이 없다면 추가
        if (host != null && !host.getChatRooms().contains(this)) {
            host.getChatRooms().add(this);
        }
    }
}
