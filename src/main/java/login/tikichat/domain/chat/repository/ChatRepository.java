package login.tikichat.domain.chat.repository;

import login.tikichat.domain.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long>, CustomChatRepository {
}
