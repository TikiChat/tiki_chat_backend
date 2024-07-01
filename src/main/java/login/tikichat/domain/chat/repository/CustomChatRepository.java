package login.tikichat.domain.chat.repository;

import login.tikichat.domain.chat.model.Chat;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomChatRepository {
    List<Chat> findChats(Long chatRoomId, Integer take, String nextCursor);
}
