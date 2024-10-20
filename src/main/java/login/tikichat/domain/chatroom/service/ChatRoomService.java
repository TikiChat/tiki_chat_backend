package login.tikichat.domain.chatroom.service;

import login.tikichat.domain.attachment.model.Attachment;
import login.tikichat.domain.attachment.model.ChatRoomAttachment;
import login.tikichat.domain.category.dto.FindCategoryDto;
import login.tikichat.domain.category.repository.CategoryRepository;
import login.tikichat.domain.chatroom.constants.ChatRoomSortType;
import login.tikichat.domain.chatroom.dto.CreateChatRoomDto;
import login.tikichat.domain.chatroom.dto.FindChatRoomDto;
import login.tikichat.domain.chatroom.model.ChatRoom;
import login.tikichat.domain.chatroom.repository.ChatRoomRepository;
import login.tikichat.domain.chatroom_participant.service.ChatRoomParticipantService;
import login.tikichat.domain.host.model.Host;
import login.tikichat.domain.host.repository.HostRepository;
import login.tikichat.domain.top_ranked_chatroom.dao.CountRankedChatRoomDao;
import login.tikichat.domain.user.model.User;
import login.tikichat.domain.user.repository.UserRepository;
import login.tikichat.domain.user.service.UserStatusService;
import login.tikichat.global.exception.BusinessException;
import login.tikichat.global.exception.ErrorCode;
import login.tikichat.global.exception.chatroom.ChatRoomNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final CategoryRepository categoryRepository;
    private final ChatRoomParticipantService chatRoomParticipantService;
    private final HostRepository hostRepository;
    private final UserRepository userRepository;
    private final CountRankedChatRoomDao chatRoomDao;
    private final UserStatusService userStatusService;


    @Transactional
    public CreateChatRoomDto.CreateChatRoomRes createChatRoom(
            Long hostUserId,
            CreateChatRoomDto.CreateChatRoomReq createChatRoomReq
    ) {
        final var category = this.categoryRepository.findByCode(
                createChatRoomReq.categoryCode()
        ).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CATEGORY));

        User user = this.userRepository.findById(hostUserId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));

        final Host host = findHost(hostUserId, user);

        final var chatRoom = new ChatRoom(
                host,
                createChatRoomReq.name(),
                createChatRoomReq.maxUserCount(),
                user.getImageUrl(),
                createChatRoomReq.tags(),
                category
        );

        chatRoom.setHost(host);

        this.chatRoomRepository.save(chatRoom);

        this.chatRoomParticipantService.joinChatRoom(chatRoom.getId(), host.getUser().getId());

        return new CreateChatRoomDto.CreateChatRoomRes(
                chatRoom.getId(),
                host.getId()
        );
    }

    private Host findHost(Long hostUserId, User user) {
        final var hostOptional = this.hostRepository.findByHostUserId(hostUserId);
        final Host host;

        if (hostOptional.isPresent()) {
            host = hostOptional.get();
        } else {
            host = Host.builder()
                    .user(user)
                    .build();
            this.hostRepository.save(host);
        }
        return host;
    }

    public FindChatRoomDto.FindChatRoomRes findChatRooms(
            FindChatRoomDto.FindChatRoomReq findChatRoomReq,
            Long userId
    ) {
        final var chatRooms = this.chatRoomRepository.findChatRooms(findChatRoomReq, userId);

        FindChatRoomDto.FindChatRoomRes findChatRoomRes = toResponse(chatRooms);

        List<ChatRoomSortType> sortPriority = Optional.ofNullable(findChatRoomReq.sortPriority())
                .orElse(Collections.emptyList());

        if (!sortPriority.isEmpty()) {
            findChatRoomRes = sortChatRooms(findChatRoomReq, findChatRoomRes);
        }

        return findChatRoomRes;
    }

    private FindChatRoomDto.FindChatRoomRes sortChatRooms(FindChatRoomDto.FindChatRoomReq findChatRoomReq, FindChatRoomDto.FindChatRoomRes findChatRoomRes) {
        Comparator<FindChatRoomDto.FindChatRoomItemRes> comparator = Comparator.comparing((FindChatRoomDto.FindChatRoomItemRes chatroom) -> 0);

        for (ChatRoomSortType sortType : findChatRoomReq.sortPriority()) {
            switch (sortType) {
                case BOOKMARK_EXIST: // 북마크가 있는 채팅방이 우선, 북마크 설정 시간 순으로 정렬
                    comparator = comparator
                            .thenComparing(FindChatRoomDto.FindChatRoomItemRes::isBookmarked, Comparator.reverseOrder()) // 북마크 여부 우선 정렬
                            .thenComparing(FindChatRoomDto.FindChatRoomItemRes::bookmarkSetTime, Comparator.nullsLast(Comparator.reverseOrder())); // 북마크 생성 시간 기준 정렬
                    break;
                case UNREAD_CHAT_EXIST: // 미읽음 메시지가 있는 방이 우선, 최근 미읽음 메시지 시간 순으로 정렬
                    comparator = comparator
                            .thenComparing((chatroom1, chatroom2) -> {
                                boolean chatroom1HasUnread = Optional.ofNullable(chatroom1.unreadChatCount()).orElse(0) > 0;
                                boolean chatroom2HasUnread = Optional.ofNullable(chatroom2.unreadChatCount()).orElse(0) > 0;
                                return Boolean.compare(chatroom2HasUnread, chatroom1HasUnread); // 미읽음 메시지가 있는 방이 우선
                            });
                    break;
                case LAST_CHAT_TIME: // 최근 메시지 발생 시간 순으로 정렬
                    comparator = comparator.thenComparing(FindChatRoomDto.FindChatRoomItemRes::lastChatTime, Comparator.nullsLast(Comparator.reverseOrder()));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid sort type");
            }
        }

        List<FindChatRoomDto.FindChatRoomItemRes> findChatRoomItemRes = findChatRoomRes.chatRooms();
        findChatRoomItemRes.sort(comparator);
        return new FindChatRoomDto.FindChatRoomRes(findChatRoomItemRes);
    }

    public ChatRoom findById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId).orElseThrow(ChatRoomNotFoundException::new);
    }

    @Transactional
    public void linkAttachment(Long chatRoomId, ChatRoomAttachment attachment) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(ChatRoomNotFoundException::new);
        chatRoom.addAttachment(attachment);
    }

    public FindChatRoomDto.FindChatRoomRes findMessageCountRankedChatRooms(
            FindChatRoomDto.FindChatRoomByPopularityReq findChatRoomReq
    ) {
        final var start = findChatRoomReq.pageNumber() * findChatRoomReq.pageSize();
        final var end = start + findChatRoomReq.pageSize() - 1;
        final var type = "message";
        final var key = getKeyForRanking(findChatRoomReq.categoryCode(), type);
        final var chatRooms = getRankedChatRooms(key, start, end);

        return toResponse(chatRooms);
    }

    public FindChatRoomDto.FindChatRoomRes findMemberCountRankedChatRooms(
            FindChatRoomDto.FindChatRoomByPopularityReq findChatRoomReq
    ) {
        final var start = findChatRoomReq.pageNumber() * findChatRoomReq.pageSize();
        final var end = start + findChatRoomReq.pageSize() - 1;
        final var type = "member";
        final var key = getKeyForRanking(findChatRoomReq.categoryCode(), type);
        final var chatRooms = getRankedChatRooms(key, start, end);

        return toResponse(chatRooms);
    }

    private String getKeyForRanking(String categoryCode, String type) {
        if (categoryCode != null) {
            return "rank:category:" + categoryCode + ":" + type + ":chatroom";
        } else {
            return "rank:total:" + type + ":chatroom";
        }
    }

    private List<ChatRoom> getRankedChatRooms(String key, int start, int end) {
        List<String> chatRoomIds = chatRoomDao.getChatRoomRank(key, start, end);

        return chatRoomRepository.findByIdsInOrder(
                chatRoomIds.stream().map(Long::parseLong).collect(Collectors.toList())
        );
    }

    private FindChatRoomDto.FindChatRoomRes toResponse(List<ChatRoom> chatRooms) {
        List<FindChatRoomDto.FindChatRoomItemRes> chatRoomItems = IntStream.range(0, chatRooms.size())
                .mapToObj(index -> {
                    ChatRoom chatRoom = chatRooms.get(index);
                    Boolean hostOnlineStatus = userStatusService.getUserStatus(chatRoom.getHost().getUser().getId()).orElse(false);

                    return new FindChatRoomDto.FindChatRoomItemRes(
                            chatRoom.getName(),
                            chatRoom.getMaxUserCount(),
                            chatRoom.getCurrentUserCount(),
                            chatRoom.getTags(),
                            chatRoom.getImageUrl(),
                            new FindCategoryDto.FindCategoryItemRes(
                                    chatRoom.getCategory().getCode(),
                                    chatRoom.getCategory().getName(),
                                    chatRoom.getCategory().getOrderNum()
                            ),
                            index + 1,
                            chatRoom.isRoomClosed(),
                            hostOnlineStatus,
                            false,                                             // TODO: 실제 값으로 수정
                            Instant.now().minus(4, ChronoUnit.HOURS),      // TODO: 실제 값으로 수정
                            32,                                                             // TODO: 실제 값으로 수정
                            Instant.now().minus(3, ChronoUnit.HOURS)       // TODO: 실제 값으로 수정
                    );
                })
                .collect(Collectors.toList());

        return new FindChatRoomDto.FindChatRoomRes(chatRoomItems);
    }
}
