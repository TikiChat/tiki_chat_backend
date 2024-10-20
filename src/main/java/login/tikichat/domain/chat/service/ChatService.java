package login.tikichat.domain.chat.service;

import login.tikichat.domain.attachment.service.AttachmentService;
import login.tikichat.domain.chat.constants.ChatReactionType;
import login.tikichat.domain.chat.dto.ModifyReactionChatEventDto;
import login.tikichat.domain.chat.model.ChatReaction;
import login.tikichat.domain.chat.pubsub.SendChatProducer;
import login.tikichat.domain.chat.dto.FindChatsDto;
import login.tikichat.domain.chat.dto.SendChatEventDto;
import login.tikichat.domain.chat.dto.SendMessageDto;
import login.tikichat.domain.chat.model.Chat;
import login.tikichat.domain.chat.repository.ChatReactionRepository;
import login.tikichat.domain.chat.repository.ChatRepository;
import login.tikichat.domain.chatroom.service.ChatRoomCommonService;
import login.tikichat.domain.chatroom_participant.service.ChatRoomParticipantService;
import login.tikichat.domain.user.service.UserCommonService;
import login.tikichat.global.exception.BusinessException;
import login.tikichat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.ast.tree.expression.Collation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final SendChatProducer sendChatProducer;
    private final ChatReactionRepository chatReactionRepository;
    private final UserCommonService userCommonService;
    private final ChatRoomCommonService chatRoomCommonService;
    private final ChatRoomParticipantService chatRoomParticipantService;
    private final ChatCommonService chatCommonService;
    private final AttachmentService attachmentService;

    @Transactional
    public SendMessageDto.SendMessageResDto sendMessage(
            Long userId,
            Long chatRoomId,
            SendMessageDto.SendMessageReqDto sendMessageReqDto
    ) throws IOException {
        final var chatRoom = this.chatRoomCommonService.findById(chatRoomId);

        this.chatRoomParticipantService.existsJoinChatRoom(chatRoomId, userId);

        final var parentChat = sendMessageReqDto.parentChatId() != null ? this.chatCommonService.findById(sendMessageReqDto.parentChatId()) : null;
        if (Objects.nonNull(sendMessageReqDto.parentChatId())) {
            if (!parentChat.getChatRoom().equals(chatRoom)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_CHAT);
            }
        }

        final var chat = Chat.sendMessage(
                userId,
                chatRoom,
                sendMessageReqDto.content(),
                sendMessageReqDto.parentChatId()
        );

        this.chatRepository.saveAndFlush(chat);

        if (sendMessageReqDto.images() != null && !sendMessageReqDto.images().isEmpty()) {
            for (final var image : sendMessageReqDto.images()) {
                this.attachmentService.uploadChatFile(
                        userId,
                        chat.getId(),
                        image
                );
            }
        }

        final List<String> imageUrls = !chat.getAttachments().isEmpty() ?
                chat.getAttachments().stream().map((chatAttachment ->
                        this.attachmentService.getAttachmentUrl(chatAttachment.getId())
                )).collect(Collectors.toList()) : Collections.emptyList();

        this.sendChatProducer.sendMessage(
                new SendChatEventDto(
                    chat.getId(),
                    chat.getContent(),
                    chat.getSenderUserId(),
                    chat.getChatRoom().getId(),
                    chat.getCreatedDate(),
                    parentChat != null ? new SendChatEventDto.SendChatParentDto(
                            parentChat.getId(),
                            parentChat.getContent(),
                            parentChat.getSenderUserId(),
                            parentChat.getCreatedDate()
                    ) : null,
                    imageUrls
                )
        );

        return new SendMessageDto.SendMessageResDto(
                chat.getId(),
                chat.getContent(),
                chat.getCreatedDate(),
                imageUrls
        );
    }

    public FindChatsDto.FindChatsRes findChats(
            Long userId,
            Long chatRoomId,
            FindChatsDto.FindChatsReq findChatsReq
    ) {
        this.chatRoomParticipantService.existsJoinChatRoom(chatRoomId, userId);

        final var chats = this.chatRepository.findChats(
                chatRoomId,
                findChatsReq.take(),
                findChatsReq.nextCursor()
        );

        return new FindChatsDto.FindChatsRes(
                chats.stream().map((chat) -> {
                    final var chatReactions = chat.getChatReactions();
                    final var chatCounts = new HashMap<ChatReactionType, List<Long>>();

                    for (final var chatReaction : chatReactions) {
                        chatCounts.computeIfAbsent(
                                chatReaction.getChatReactionType(),
                                (k) -> new ArrayList<>()
                        ).add(chatReaction.getUser().getId());
                    }

                    final List<String> imageUrls = !chat.getAttachments().isEmpty() ?
                            chat.getAttachments().stream().map((chatAttachment ->
                                    this.attachmentService.getAttachmentUrl(chatAttachment.getId())
                            )).collect(Collectors.toList()) : Collections.emptyList();

                    final List<FindChatsDto.FindChatReactionListRes> reactions = new ArrayList<>();
                    chatCounts.forEach((chatReactionType, list) -> {
                                reactions.add(new FindChatsDto.FindChatReactionListRes(
                                        list,
                                        chatReactionType,
                                        list.size()
                                ));
                            }
                    );

                    final var parentChat = chat.getParentChatId() != null ? this.chatCommonService.findById(chat.getParentChatId()) : null;

                    return new FindChatsDto.FindChatsItemRes(
                            chat.getId(),
                            chat.getContent(),
                            chat.getCreatedDate(),
                            reactions,
                            parentChat != null ? new FindChatsDto.FindChatsParentItemRes(
                                    parentChat.getId(),
                                    parentChat.getContent(),
                                    parentChat.getCreatedDate()
                            ) : null,
                            imageUrls
                    );
                }
                ).toList(),
                chats.isEmpty() ? null : chats.get(chats.size()-1).getId()
        );
    }

    @Transactional
    public Long addChatReaction(Long chatId,
                                Long userId,
                                ChatReactionType chatReactionType
    ) {
        final var user = this.userCommonService.findById(userId);
        final var chat = this.chatCommonService.findById(chatId);

        this.chatRoomParticipantService.existsJoinChatRoom(
                chat.getChatRoom().getId(), userId
        );

        if (this.chatReactionRepository.findByChatIdAndUserIdAndChatReactionType(
                chat.getId(),
                user.getId(),
                chatReactionType
        ).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS_REACTION_CHAT);
        };

        final var chatReaction = new ChatReaction(user, chat, chatReactionType);

        chatReactionRepository.save(chatReaction);

        this.sendChatProducer.modifyReaction(
                new ModifyReactionChatEventDto(
                        chat.getId(),
                        chatReaction.getChat().getId(),
                        chatReaction.getUser().getId(),
                        chatReaction.getChatReactionType(),
                        true
                )
        );

        return chatReaction.getId();
    }

    @Transactional
    public void removeChatReaction(Long chatId,
                                Long userId,
                                ChatReactionType chatReactionType
    ) {
        final var chat = this.chatCommonService.findById(chatId);

        this.chatRoomParticipantService.existsJoinChatRoom(
                chat.getChatRoom().getId(), userId
        );

        final var chatReaction = this.chatReactionRepository.findByChatIdAndUserIdAndChatReactionType(
                chatId,
                userId,
                chatReactionType
        ).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CHAT));

        this.sendChatProducer.modifyReaction(
                new ModifyReactionChatEventDto(
                        chat.getId(),
                        chatReaction.getChat().getId(),
                        chatReaction.getUser().getId(),
                        chatReaction.getChatReactionType(),
                        false
                )
        );

        this.chatReactionRepository.deleteByChatIdAndUserIdAndChatReactionType(
                chatId,
                userId,
                chatReactionType
        );
    }
}
