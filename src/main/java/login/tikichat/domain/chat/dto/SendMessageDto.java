package login.tikichat.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

public class SendMessageDto {
    public record SendMessageResDto(
        Long id,
        String content,
        Instant createdAt,
        List<String> imageUrls
    ) {
    }

    public record SendMessageReqDto (
        @Length(min = 1, max = 1000)
        @Schema(minLength = 1, maxLength = 1000)
        String content,
            @Schema(
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true
        )
        Long parentChatId,
        @Schema(
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true
        )
        List<MultipartFile> images
    ) {

    }
}
