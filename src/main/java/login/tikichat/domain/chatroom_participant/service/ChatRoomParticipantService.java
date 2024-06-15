package login.tikichat.domain.chatroom_participant.service;

import login.tikichat.domain.chatroom.repository.ChatRoomRepository;
import login.tikichat.domain.chatroom_participant.model.ChatRoomParticipant;
import login.tikichat.domain.chatroom_participant.repository.ChatRoomParticipantRepository;
import login.tikichat.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ChatRoomParticipantService {
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void joinChatRoom(Long chatRoomId, Long userId) {
        final var chatRoom = this.chatRoomRepository.findById(chatRoomId).orElseThrow();
        final var user = this.userRepository.findById(userId).orElseThrow();

        final var chatRoomParticipant = new ChatRoomParticipant(user, chatRoom);

        chatRoomRepository.addCurrentUserCount(chatRoom.getId());

        this.chatRoomParticipantRepository.save(chatRoomParticipant);
    }

    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long userId) {
        final var chatRoom = this.chatRoomRepository.findById(chatRoomId).orElseThrow();
        final var user = this.userRepository.findById(userId).orElseThrow();
        final var chatRoomParticipant = this.chatRoomParticipantRepository.findByUserAndChatRoom(user, chatRoom).orElseThrow();

        chatRoomRepository.subtractCurrentUserCount(chatRoom.getId());

        this.chatRoomParticipantRepository.delete(chatRoomParticipant);
    }
}