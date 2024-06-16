package login.tikichat.domain.chatroom.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import login.tikichat.domain.chatroom.dto.CreateChatRoomDto;
import login.tikichat.domain.chatroom.dto.FindChatRoomDto;
import login.tikichat.domain.chatroom.service.ChatRoomService;
import login.tikichat.global.auth.UserDetailInfo;
import login.tikichat.global.response.ResultCode;
import login.tikichat.global.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "JWT")
@RestController
@Tag(name = "ChatRoom", description = "채팅방")
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping(
            value = ""
    )
    public ResponseEntity<ResultResponse> createChatRoom(
            @RequestBody
            @Valid
            @Parameter(required = true)
            CreateChatRoomDto.CreateChatRoomReq createChatRoomReq,
            @AuthenticationPrincipal UserDetailInfo user
    ) {
        ResultResponse result = ResultResponse.of(
                ResultCode.FIND_USER_INFO_SUCCESS,
                new CreateChatRoomDto.CreateChatRoomRes(
                        this.chatRoomService.createChatRoom(user.getUserId(), createChatRoomReq)
                )
        );
        return new ResponseEntity<>(result, HttpStatus.valueOf(result.getStatus()));
    }

    @GetMapping("")
    public ResponseEntity<ResultResponse> findChatRooms(
            @RequestParam @Valid FindChatRoomDto.FindChatRoomReq findChatRoomReq,
            @AuthenticationPrincipal UserDetailInfo user
    ) {
        ResultResponse result = ResultResponse.of(
                ResultCode.FIND_USER_INFO_SUCCESS,
                this.chatRoomService.findChatRooms(findChatRoomReq, user.getUserId())
        );
        return new ResponseEntity<>(result, HttpStatus.valueOf(result.getStatus()));
    }

    @GetMapping("/ranked")
    public ResponseEntity<ResultResponse> findRankedChatRooms(
            @RequestParam @Valid FindChatRoomDto.FindChatRoomByPopularityReq findChatRoomReq,
            @AuthenticationPrincipal UserDetailInfo user
    ) {
        ResultResponse result = ResultResponse.of(
                ResultCode.FIND_CHAT_ROOMS_SUCCESS,
                this.chatRoomService.findChatRoomsByPopularity(findChatRoomReq, user.getUserId())
        );
        return new ResponseEntity<>(result, HttpStatus.valueOf(result.getStatus()));
    }
}
