package login.tikichat.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import login.tikichat.domain.chat.dto.AddChatReactionDto;
import login.tikichat.domain.chat.dto.FindChatsDto;
import login.tikichat.domain.chat.dto.SendMessageDto;
import login.tikichat.domain.chat.service.ChatService;
import login.tikichat.global.auth.UserDetailInfo;
import login.tikichat.global.response.ResultCode;
import login.tikichat.global.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@SecurityRequirement(name = "JWT")
@RestController
@Tag(name = "Chat API", description = "채팅 API")
@RequestMapping("/api/v1/chat-rooms/{chatRoomId}/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping(value = "", consumes = { "multipart/form-data" })
    @Operation(summary = "채팅 메세지 보내기", description = "채팅 메세지를 보내는 API입니다.")
    public ResponseEntity<ResultResponse> sendChat(
            @ModelAttribute @Valid SendMessageDto.SendMessageReqDto sendMessageReqDto,
            @AuthenticationPrincipal UserDetailInfo user,
            @PathVariable("chatRoomId") Long chatRoomId
    ) throws IOException {
        ResultResponse result = ResultResponse.of(
                ResultCode.SEND_CHAT_SUCCESS,
                this.chatService.sendMessage(user.getUserId(), chatRoomId, sendMessageReqDto)
        );

        return new ResponseEntity<>(result, HttpStatus.valueOf(result.getStatus()));
    }

    @PostMapping("/{chatId}/reaction")
    @Operation(summary = "채팅 반응 보내기", description = "채팅 반응을 보내는 API입니다.")
    public void addChatReaction(
            @RequestBody @Valid AddChatReactionDto.AddChatReactionReq addChatReactionReq,
            @AuthenticationPrincipal UserDetailInfo user,
            @PathVariable("chatId") Long chatId,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        this.chatService.addChatReaction(
                chatId,
                user.getUserId(),
                addChatReactionReq.chatReactionType()
        );
    }

    @DeleteMapping("/{chatId}/reaction")
    @Operation(summary = "채팅 반응 취소하기", description = "채팅 반응을 취소하는 API입니다.")
    public void removeChatReaction(
            @RequestBody @Valid AddChatReactionDto.AddChatReactionReq addChatReactionReq,
            @AuthenticationPrincipal UserDetailInfo user,
            @PathVariable("chatId") Long chatId,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        this.chatService.removeChatReaction(
                chatId,
                user.getUserId(),
                addChatReactionReq.chatReactionType()
        );
    }

    @GetMapping("")
    @Operation(summary = "채팅방 메세지 목록 조회", description = "채팅방 메세지 목록을 조회하는 API입니다.")
    public ResponseEntity<ResultResponse> getChats(
            @Valid FindChatsDto.FindChatsReq findChatsReq,
            @AuthenticationPrincipal UserDetailInfo user,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        ResultResponse result = ResultResponse.of(
                ResultCode.FIND_CHAT_LIST_SUCCESS,
                this.chatService.findChats(user.getUserId(), chatRoomId, findChatsReq)
        );

        return new ResponseEntity<>(result, HttpStatus.valueOf(result.getStatus()));
    }
}
