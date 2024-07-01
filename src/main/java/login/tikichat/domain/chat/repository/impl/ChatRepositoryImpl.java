package login.tikichat.domain.chat.repository.impl;

import login.tikichat.domain.chat.model.Chat;
import login.tikichat.domain.chat.model.QChat;
import login.tikichat.domain.chat.repository.CustomChatRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatRepositoryImpl extends QuerydslRepositorySupport implements CustomChatRepository {
    public ChatRepositoryImpl() {
        super(Chat.class);
    }

    @Override
    public List<Chat> findChats(Long chatRoomId, Integer take, String nextCursor) {
        final var chatQ = QChat.chat;
        final var query = super.from(chatQ);

        query.where(chatQ.chatRoomId.eq(chatRoomId));
        query.limit(take);
        query.orderBy(chatQ.id.desc());

        if (nextCursor != null) {
            query.where(chatQ.id.lt(nextCursor));
        }

        return query.fetch();
    }
}
