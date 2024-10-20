package login.tikichat.global.config;

import login.tikichat.domain.category.model.Category;
import login.tikichat.domain.category.repository.CategoryRepository;
import login.tikichat.domain.chat.model.Chat;
import login.tikichat.domain.chat.model.ChatReaction;
import login.tikichat.domain.chat.repository.ChatRepository;
import login.tikichat.domain.chatroom.model.ChatRoom;
import login.tikichat.domain.chatroom.repository.ChatRoomRepository;
import login.tikichat.domain.host.model.Host;
import login.tikichat.domain.host.repository.HostRepository;
import login.tikichat.domain.terms.dto.TermsCreateRequest;
import login.tikichat.domain.terms.model.TermsType;
import login.tikichat.domain.terms.service.TermsService;
import login.tikichat.domain.user.model.Role;
import login.tikichat.domain.user.model.SocialProfile;
import login.tikichat.domain.user.model.SocialType;
import login.tikichat.domain.user.model.User;
import login.tikichat.domain.user.repository.SocialProfileRepository;
import login.tikichat.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 초기 상태 등록 Config
 */
@Configuration
@RequiredArgsConstructor
@Profile(value = "!test") // test 에서는 제외
public class InitializeDefaultConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SocialProfileRepository socialProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TermsService termsService;
    private final CategoryRepository categoryRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final HostRepository hostRepository;

    private static final int CHAT_ROOM_COUNT = 50;
    private static final int CHAT_COUNT = 700;

    /**
     * 각종 엔티티들 임시 초기화 수행(개발계 테스트용)
     */
    @Override
    public void run(String... args) throws Exception {
        User user = initializeUser();
        initializeSocialProfile(user);
        List<Category> categories = initializeCategories();
        Host host = initializeHost(user);
        List<ChatRoom> chatRooms = initializeChatRooms(categories, host);
        ChatReaction chatReaction1 = initializeChatReaction();
        initializeChats(chatRooms, chatReaction1);
        initializeTerms();
    }

    private User initializeUser() throws MalformedURLException {
        User user = User.builder()
                .id(1L)
                .email("dydydade@gmail.com")
                .nickname("dydydade")
                .imageUrl(new URL("https://tiki-chat-bucket.s3.ap-southeast-2.amazonaws.com/profile_default.png"))
                .role(Role.SOCIAL)
                .build();
        User user2 = User.builder()
                .id(2L)
                .email("n4oahdev@gmail.com")
                .nickname("n4oahdev")
                .imageUrl(new URL("https://tiki-chat-bucket.s3.ap-southeast-2.amazonaws.com/profile_default.png"))
                .role(Role.USER)
                .password(passwordEncoder.encode("1234"))
                .build();

        userRepository.save(user);
        userRepository.save(user2);
        return user;
    }

    private void initializeSocialProfile(User user) {
        SocialProfile naver = SocialProfile.builder()
                .id(1L)
                .socialEmail("xx5882@naver.com")
                .socialType(SocialType.NAVER)
                .socialId("id")
                .user(user)
                .build();
        SocialProfile kakao = SocialProfile.builder()
                .id(2L)
                .socialEmail("xx5882@naver.com")
                .socialType(SocialType.KAKAO)
                .socialId("id")
                .user(user)
                .build();
        SocialProfile google = SocialProfile.builder()
                .id(3L)
                .socialEmail("dydydade@gmail.com")
                .socialType(SocialType.GOOGLE)
                .socialId("sub")
                .user(user)
                .build();
    }

    private List<Category> initializeCategories() {
        List<Category> categories = List.of(
                new Category("C_1001", "썸 · 연애", 1),
                new Category("C_1002", "다이어트 · 헬스", 2),
                new Category("C_1003", "직장인/취업", 3),
                new Category("C_1004", "공부/대입", 4),
                new Category("C_1005", "자기계발/재테크", 5),
                new Category("C_1006", "가족 · 결혼", 6),
                new Category("C_1007", "연예인/팬", 7),
                new Category("C_1008", "드라마 · 영화", 8),
                new Category("C_1009", "뷰티/패션", 9),
                new Category("C_1010", "취미", 10)
        );
        categoryRepository.saveAll(categories);
        return categories;
    }

    private Host initializeHost(User user) {
        Host host = new Host(user, null, null);
        host.setId(1L);
        hostRepository.save(host);
        return host;
    }

    private List<ChatRoom> initializeChatRooms(List<Category> categories, Host host) throws MalformedURLException {
        List<ChatRoom> chatRooms = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < CHAT_ROOM_COUNT; i++) {
            ChatRoom chatRoom = new ChatRoom(host, "채팅방" + String.valueOf(i), 200, new URL("https://tiki-chat-bucket.s3.ap-southeast-2.amazonaws.com/profile_default.png"), List.of("고민"), categories.get(random.nextInt(10)));
            chatRooms.add(chatRoom);
        }

        chatRoomRepository.saveAll(chatRooms);
        return chatRooms;
    }

    private static ChatReaction initializeChatReaction() {
        ChatReaction chatReaction1 = new ChatReaction();
        return chatReaction1;
    }

    private void initializeChats(List<ChatRoom> chatRooms, ChatReaction chatReaction1) {
        int batchSize = 50; // 한 번에 저장할 데이터의 수
        int sleepTimeMillis = 200; // 각 배치 사이의 대기 시간 (밀리초)

        List<Chat> chats = new ArrayList<>();
        Random random = new Random();

        for (long i = 0; i < CHAT_COUNT; i++) {
            int randNum = random.nextInt(CHAT_ROOM_COUNT);
            Chat chat = new Chat(i, "content" + i, 1L, chatRooms.get(randNum), Instant.now(), Set.of(chatReaction1), null, null);
            chats.add(chat);

            if (chats.size() >= batchSize) {
                chatRepository.saveAll(chats);
                chatRepository.flush();
                chats.clear(); // 리스트 초기화
                System.out.println("배치 수행중... " + (i + 1) + " 번째 데이터까지 저장 완료");

                // 각 배치 사이에 일정 시간 대기
                try {
                    Thread.sleep(sleepTimeMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("스레드 대기 중 오류 발생");
                }
            }
        }

        // 남은 데이터 저장
        if (!chats.isEmpty()) {
            chatRepository.saveAll(chats);
            chatRepository.flush();
        }
    }

    private void initializeTerms() {
        // 이용약관 저장
        TermsCreateRequest termsOfService = TermsCreateRequest.builder()
                .termsType(TermsType.TERMS_OF_SERVICE)
                .version("1.0")
                .content("서비스 이용 동의입니다.")
                .mandatory(true)
                .effectiveDate(LocalDate.now())
                .build();
        TermsCreateRequest privacyPolicy = TermsCreateRequest.builder()
                .termsType(TermsType.PRIVACY_POLICY)
                .version("1.0")
                .content("개인정보 이용약관입니다.")
                .mandatory(true)
                .effectiveDate(LocalDate.now())
                .build();

        termsService.createTerms(termsOfService);
        termsService.createTerms(privacyPolicy);
    }
}
