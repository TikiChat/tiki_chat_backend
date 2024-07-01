package login.tikichat.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
            String nextCursor
    ) {
        public FindChatsReq() {
            this(
                    20, null
            );
        }
    }

    public record FindChatsItemRes (
            String id,
            String content,
            Instant createdAt
    ) {

    }

    public record FindChatsRes (
            List<FindChatsItemRes> chats,
            @Schema(nullable = true)
            String nextCursor
    ) {
    }

}
