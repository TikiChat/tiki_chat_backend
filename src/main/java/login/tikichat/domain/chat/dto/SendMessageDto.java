package login.tikichat.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

public class SendMessageDto {
    public record SendMessageResDto(
        String id,
        String content,
        Instant createdAt
    ) {
    }

    public record SendMessageReqDto (
        @Length(min = 1, max = 1000)
        @Schema(minLength = 1, maxLength = 1000)
        String content
    ) {

    }
}
