package login.tikichat.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import login.tikichat.domain.chat.constants.ChatReactionType;

import java.time.Instant;
import java.util.List;

public class FindChatsDto {
    public record FindChatsReq (
            @Schema(minimum = "1", maximum = "50", nullable = true, defaultValue = "20", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            @Min(1)
            @Max(50)
            @Nullable
            Integer take,
            @Nullable
            @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            Long nextCursor
    ) {
        public FindChatsReq {
            if(take == null) {
                take = 20;
            }
        }
    }

    public record FindChatReactionListRes(
        List<Long> userIds,
        ChatReactionType reactionType,
        Integer count
    ) {

    }

    public record FindChatsItemRes (
            Long id,
            String content,
            Instant createdAt,
            List<FindChatReactionListRes> reactions,
            FindChatsParentItemRes parentChat,
            List<String> iamgeUrls
    ) {

    }

    public record FindChatsParentItemRes (
            Long id,
            String content,
            Instant createdAt
    ) {

    }

    public record FindChatsRes (
            List<FindChatsItemRes> chats,
            @Schema(nullable = true)
            Long nextCursor
    ) {
    }

}
