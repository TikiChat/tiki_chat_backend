package login.tikichat.domain.chat.service;

import login.tikichat.domain.chat.pubsub.SendChatProducer;
import login.tikichat.domain.chat.dto.FindChatsDto;
import login.tikichat.domain.chat.dto.SendChatEventDto;
import login.tikichat.domain.chat.dto.SendMessageDto;
import login.tikichat.domain.chat.model.Chat;
import login.tikichat.domain.chat.repository.ChatRepository;
import login.tikichat.domain.chatroom.repository.ChatRoomRepository;
import login.tikichat.domain.chatroom_participant.repository.ChatRoomParticipantRepository;
import login.tikichat.domain.user.repository.UserRepository;
import login.tikichat.global.exception.BusinessException;
import login.tikichat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SendChatProducer sendChatProducer;

    @Transactional
    public SendMessageDto.SendMessageResDto sendMessage(
            Long userId,
            Long chatRoomId,
            SendMessageDto.SendMessageReqDto sendMessageReqDto
    ) {
        final var user = this.userRepository.findById(userId).orElseThrow(() ->
                new BusinessException(ErrorCode.NOT_FOUND_USER)
        );
        final var chatRoom = this.chatRoomRepository.findById(chatRoomId).orElseThrow(() ->
                new BusinessException(ErrorCode.NOT_FOUND_CHAT_ROOM)
        );

        this.chatRoomParticipantRepository.findByUserAndChatRoom(
                user,
                chatRoom
        ).orElseThrow(() ->
                new BusinessException(ErrorCode.NOT_CHAT_ROOM_PARTICIPANT)
        );

        final var chat = Chat.sendMessage(
                userId,
                chatRoomId,
                sendMessageReqDto.content()
        );

        this.chatRepository.saveAndFlush(chat);

        this.sendChatProducer.sendMessage(
                new SendChatEventDto(
                    chat.getId(),
                    chat.getContent(),
                    chat.getSenderUserId(),
                    chat.getChatRoomId(),
                    chat.getCreatedDate()
                )
        );

        return new SendMessageDto.SendMessageResDto(
                chat.getId(),
                chat.getContent(),
                chat.getCreatedDate()
        );
    }

    public FindChatsDto.FindChatsRes findChats(
            Long userId,
            Long chatRoomId,
            FindChatsDto.FindChatsReq findChatsReq
    ) {
        final var user = this.userRepository.findById(userId).orElseThrow(() ->
                new BusinessException(ErrorCode.NOT_FOUND_USER)
        );
        final var chatRoom = this.chatRoomRepository.findById(chatRoomId).orElseThrow(() ->
                new BusinessException(ErrorCode.NOT_FOUND_CHAT_ROOM)
        );

        this.chatRoomParticipantRepository.findByUserAndChatRoom(
                user,
                chatRoom
        ).orElseThrow(() ->
                new BusinessException(ErrorCode.NOT_CHAT_ROOM_PARTICIPANT)
        );

        final var chats = this.chatRepository.findChats(
                chatRoomId,
                findChatsReq.take(),
                findChatsReq.nextCursor()
        );

        return new FindChatsDto.FindChatsRes(
                chats.stream().map((chat) ->
                    new FindChatsDto.FindChatsItemRes(
                            chat.getId(),
                            chat.getContent(),
                            chat.getCreatedDate()
                    )
                ).toList(),
                chats.isEmpty() ? null : chats.get(chats.size()-1).getId()
        );
    }
}
