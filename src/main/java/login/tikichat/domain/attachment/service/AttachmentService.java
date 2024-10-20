package login.tikichat.domain.attachment.service;

import login.tikichat.domain.attachment.model.Attachment;
import login.tikichat.domain.attachment.model.ChatAttachment;
import login.tikichat.domain.attachment.model.ChatRoomAttachment;
import login.tikichat.domain.attachment.repository.AttachmentRepository;
import login.tikichat.domain.chat.model.Chat;
import login.tikichat.domain.chat.repository.ChatRepository;
import login.tikichat.domain.chatroom.model.ChatRoom;
import login.tikichat.domain.chatroom.service.ChatRoomService;
import login.tikichat.domain.user.repository.UserRepository;
import login.tikichat.global.component.FileStorage;
import login.tikichat.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final ChatRepository chatRepository;

    @Transactional
    public Long uploadChatRoomFile(Long uploaderUserId, Long chatRoomId, MultipartFile multipartFile) throws IOException {
        final var ext = FileUtils.getExtByContentType(multipartFile.getContentType());
        final var uploader = userRepository.findById(uploaderUserId).orElseThrow();

        final var path = "chat-room/" + FileUtils.getTimePath();  // 채팅방용 디렉토리 구분
        final var filename = FileUtils.getRandomFilename(ext);

        ChatRoom chatRoom = chatRoomService.findById(chatRoomId);

        fileStorage.upload(path + "/" + filename, multipartFile.getInputStream());

        final var attachment = new ChatRoomAttachment(
                uploader,
                chatRoom,
                path,
                filename,
                multipartFile.getOriginalFilename(),
                ext
        );
        attachmentRepository.save(attachment);

        // 채팅방에 이미지 첨부 정보 링크
        chatRoomService.linkAttachment(chatRoomId, attachment);

        return attachment.getId();
    }

    @Transactional
    public Long uploadChatFile(Long uploaderUserId, Long chatId, MultipartFile multipartFile) throws IOException {
        final var ext = FileUtils.getExtByContentType(multipartFile.getContentType());
        final var uploader = userRepository.findById(uploaderUserId).orElseThrow();

        final var path = "chat/" + FileUtils.getTimePath();  // 채팅방용 디렉토리 구분
        final var filename = FileUtils.getRandomFilename(ext);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow();

        fileStorage.upload(path + "/" + filename, multipartFile.getInputStream());

        System.out.println("chat" + chat.getAttachments());

        final var attachment = new ChatAttachment(
                uploader,
                chat,
                path,
                filename,
                multipartFile.getOriginalFilename(),
                ext
        );
        attachmentRepository.save(attachment);

        return attachment.getId();
    }

    @Transactional(readOnly = true)
    public String getAttachmentUrl(Long attachmentId) {
        final var attachment = this.attachmentRepository.findById(attachmentId)
                .orElseThrow();

        return this.fileStorage.getUrl(
                attachment.getPath() + "/" + attachment.getFilename()
        );
    }
}

