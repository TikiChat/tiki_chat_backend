## LEMON 앱 인증 모듈 진행 흐름


### API 목록
![image](https://github.com/dydydade/oauthtest4/assets/78246372/951d1b17-d333-46ca-bc54-1b4322bee778)


<br/>

### 1. 회원 가입
입력 정보를 받아서 회원가입 진행, 결과 메세지 반환

```java
    /**
     * 회원 가입
     *
     * @param userSignUpRequest
     * @return
     */
    @PostMapping
    public ResponseEntity<?> signUp(@RequestBody UserSignUpRequest userSignUpRequest) {
        UserSignUpResponse userSignUpResponse = userService.signUp(userSignUpRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userSignUpResponse, "회원 가입이 정상적으로 완료되었습니다."));
    }
```

※ UserSignUpRequest 에 소셜 프로필 연동 정보(UserSignUpSocialProfileDto) 가 함께 넘어오면 회원 등록하면서 자동으로 소셜 로그인 연동
```java
@Getter
@NoArgsConstructor
public class UserSignUpRequest {
    private String email;

    private String password;

    private String nickname;

    private UserSignUpSocialProfileDto socialProfileDto; // <-- 이 부분 함께 넘어오면 자동으로 소셜 프로필 연동까지 진행
}
```

```java
@Getter
@NoArgsConstructor
public class UserSignUpSocialProfileDto {
    private SocialType socialType; // KAKAO, NAVER, GOOGLE, FACEBOOK

    private String socialId; // 로그인한 소셜 타입의 식별자 값

    private String socialEmail; // 인증 서버로부터 넘겨받은 소셜 email 정보
}
```

<br/>

### 2. 회원 탈퇴
```java

    /**
     * 회원 탈퇴
     *
     * @param userId      탈퇴하려는 계정의 ID
     * @param currentUser 로그인 사용자 정보 (UserDetails)
     * @return
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> signOff(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        UserSignOffResponse userSignOffResponse = userService.signOff(userId, currentUser);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(userSignOffResponse, "회원 탈퇴가 정상적으로 완료되었습니다."));
    }
```

※ 인자로 넘어온 UserDetails(로그인 사용자 정보)와 탈퇴하려는 계정의 Email이 일치하지 않으면 UnauthorizedAccountAttemptException 던짐 
```java
    /**
     * 회원 탈퇴
     * @param userId 탈퇴하려는 계정의 ID
     * @param currentUser 로그인 사용자 정보 (UserDetails)
     */
    @Transactional
    public UserSignOffResponse signOff(Long userId, UserDetails currentUser) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            throw new RegisteredUserNotFoundException();
        }

        User user = userOptional.get();

        String targetUserEmail = user.getEmail();
        String currentUserEmail = currentUser.getUsername();

        if (!targetUserEmail.equals(currentUserEmail)) {
            throw new UnauthorizedAccountAttemptException(); // <- 이부분
        }

        userRepository.deleteById(userId);
        return UserSignOffResponse.builder()
                .userId(userId)
                .build();
    }
```


<br/>

### 5. ID/PW로 로그인(일반 로그인)
진행 흐름:

① "/api/v1/auth/login" 경로로 요청이 들어올 경우, CustomJsonUsernamePasswordAuthenticationFilter(json으로 넘어온 ID/PW 정보로 인증 처리하는 필터) 에서 인증 시도

<details>
<summary>코드 보기</summary>

<!-- summary 아래 한칸 공백 두어야함 -->
```java
/**
 * 스프링 시큐리티의 폼 기반의 UsernamePasswordAuthenticationFilter를 참고하여 만든 커스텀 필터
 * 거의 구조가 같고, Type이 Json인 Login만 처리하도록 설정한 부분만 다르다. (커스텀 API용 필터 구현)
 * Username : 회원 아이디 -> email로 설정
 * "/login" 요청 왔을 때 JSON 값을 매핑 처리하는 필터
 */
public class CustomJsonUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final String DEFAULT_LOGIN_REQUEST_URL = "/api/v1/auth/login"; // "/api/v1/login"으로 오는 요청을 처리
    private static final String HTTP_METHOD = "POST"; // 로그인 HTTP 메소드는 POST
    private static final String CONTENT_TYPE = "application/json"; // JSON 타입의 데이터로 오는 로그인 요청만 처리
    private static final String USERNAME_KEY = "email"; // 회원 로그인 시 이메일 요청 JSON Key : "email"
    private static final String PASSWORD_KEY = "password"; // 회원 로그인 시 비밀번호 요청 JSon Key : "password"
    private static final AntPathRequestMatcher DEFAULT_LOGIN_PATH_REQUEST_MATCHER =
            new AntPathRequestMatcher(DEFAULT_LOGIN_REQUEST_URL, HTTP_METHOD); // "/login" + POST로 온 요청에 매칭된다.

    private final ObjectMapper objectMapper;

    public CustomJsonUsernamePasswordAuthenticationFilter(ObjectMapper objectMapper) {
        super(DEFAULT_LOGIN_PATH_REQUEST_MATCHER); // 위에서 설정한 "login" + POST로 온 요청을 처리하기 위해 설정
        this.objectMapper = objectMapper;
    }

    /**
     * 인증 처리 메소드
     *
     * UsernamePasswordAuthenticationFilter와 동일하게 UsernamePasswordAuthenticationToken 사용
     * StreamUtils를 통해 request에서 messageBody(JSON) 반환
     * 요청 JSON Example
     * {
     *    "email" : "aaa@bbb.com"
     *    "password" : "test123"
     * }
     * 꺼낸 messageBody를 objectMapper.readValue()로 Map으로 변환 (Key : JSON의 키 -> email, password)
     * Map의 Key(email, password)로 해당 이메일, 패스워드 추출 후
     * UsernamePasswordAuthenticationToken의 파라미터 principal, credentials에 대입
     *
     * AbstractAuthenticationProcessingFilter(부모)의 getAuthenticationManager()로 AuthenticationManager 객체를 반환 받은 후
     * authenticate()의 파라미터로 UsernamePasswordAuthenticationToken 객체를 넣고 인증 처리
     * (여기서 AuthenticationManager 객체는 ProviderManager -> SecurityConfig에서 설정)
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
        if(request.getContentType() == null || !request.getContentType().equals(CONTENT_TYPE)  ) {
            throw new AuthenticationServiceException("Authentication Content-Type not supported: " + request.getContentType());
        }

        String messageBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        Map<String, String> usernamePasswordMap = objectMapper.readValue(messageBody, Map.class);

        String email = usernamePasswordMap.get(USERNAME_KEY);
        String password = usernamePasswordMap.get(PASSWORD_KEY);

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(email, password);//principal 과 credentials 전달

        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
```
</details>

② 인증 성공 시 : LoginSuccessHandler 호출 → Jwt Token 생성하여 클라이언트 측으로 전송

<details>
<summary>코드 보기</summary>

<!-- summary 아래 한칸 공백 두어야함 -->
```java
@Slf4j
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${jwt.access.expiration}")
    private String accessTokenExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String email = extractUsername(authentication); // 인증 정보에서 Username(email) 추출
        String accessToken = jwtService.createAccessToken(email); // JwtService의 createAccessToken을 사용하여 AccessToken 발급
        String refreshToken = jwtService.createRefreshToken(); // JwtService의 createRefreshToken을 사용하여 RefreshToken 발급

        jwtService.sendAccessAndRefreshToken(response, accessToken, refreshToken); // 응답 헤더에 AccessToken, RefreshToken 실어서 응답

        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    user.updateRefreshToken(refreshToken);
                    userRepository.saveAndFlush(user);
                });

        log.info("로그인에 성공하였습니다. 이메일 : {}", email);
        log.info("로그인에 성공하였습니다. AccessToken : {}", accessToken);
        log.info("발급된 AccessToken 만료 기간 : {}", accessTokenExpiration);
    }

    private String extractUsername(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }
}
```
</details>

③ 인증 실패 시 : LoginFailureHandler 호출 → 클라이언트 측으로 로그인 실패 메세지 반환

<details>
<summary>코드 보기</summary>

<!-- summary 아래 한칸 공백 두어야함 -->
```java
/**
 * JWT 로그인 실패 시 처리하는 핸들러
 * SimpleUrlAuthenticationFailureHandler를 상속받아서 구현
 */
@Slf4j
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("로그인 실패! 이메일이나 비밀번호를 확인해주세요.");
        log.info("로그인에 실패했습니다. 메시지 : {}", exception.getMessage());
    }
}

```
</details>

<br/>

### 10~13. 소셜 로그인(카카오/네이버/구글/애플)
진행 흐름:

① "oauth2/authorization/{socialType}" 경로로 요청이 들어올 경우, CustomOAuth2UserService 의 loadUser() 메서드 호출

(User 엔티티를 찾아서 CustomOAuth2User 를 만들어 반환하면 인증 성공)
<details>
<summary>코드 보기</summary>

<!-- summary 아래 한칸 공백 두어야함 -->
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final SocialProfileRepository socialProfileRepository;

    private static final String NAVER = "naver";
    private static final String KAKAO = "kakao";
    private static final String FACEBOOK = "facebook";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.debug("CustomOAuth2UserService.loadUser() 실행 - OAuth2 로그인 요청 진입");

        /**
         * DefaultOAuth2UserService 객체를 생성하여, loadUser(userRequest)를 통해 DefaultOAuth2User 객체를 생성 후 반환
         * DefaultOAuth2UserService의 loadUser()는 소셜 로그인 API의 사용자 정보 제공 URI로 요청을 보내서
         * 사용자 정보를 얻은 후, 이를 통해 DefaultOAuth2User 객체를 생성 후 반환한다.
         * 결과적으로, OAuth2User는 OAuth 서비스에서 가져온 유저 정보를 담고 있는 유저
         */
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        /**
         * userRequest에서 registrationId 추출 후 registrationId으로 SocialType 저장
         * http://localhost:8080/oauth2/authorization/kakao에서 kakao가 registrationId
         * userNameAttributeName은 이후에 nameAttributeKey로 설정된다.
         */
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = getSocialType(registrationId);
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // OAuth2 로그인 시 키(PK)가 되는 값
        Map<String, Object> attributes = oAuth2User.getAttributes(); // 소셜 로그인에서 API가 제공하는 userInfo의 Json 값(유저 정보들)

        // socialType에 따라 유저 정보를 통해 OAuthAttributes 객체 생성
        OAuthAttributes extractAttributes = OAuthAttributes.of(socialType, userNameAttributeName, attributes);

        User createdUser = getUser(extractAttributes, socialType); // getUser() 메소드로 User 객체 생성 후 반환

        // DefaultOAuth2User를 구현한 CustomOAuth2User 객체를 생성해서 반환
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(createdUser.getRole().getKey())),
                attributes,
                extractAttributes.getNameAttributeKey(),
                createdUser.getEmail(),
                createdUser.getRole()
        );
    }

    private SocialType getSocialType(String registrationId) {
        if(NAVER.equals(registrationId)) {
            return SocialType.NAVER;
        }
        if(KAKAO.equals(registrationId)) {
            return SocialType.KAKAO;
        }
        if(FACEBOOK.equals(registrationId)) {
            return SocialType.FACEBOOK;
        }
        return SocialType.GOOGLE;
    }
}
```
</details>

② loadUser() 는 내부적으로 getUser() 메서드 호출 - 인증 서버로부터 넘겨받은 로그인 정보를 통해 User 엔티티를 찾아 반환하는 메서드
<details>
<summary>코드 보기</summary>

<!-- summary 아래 한칸 공백 두어야함 -->
```java
/**
 * SocialType과 attributes에 들어있는 소셜 로그인의 식별값 id를 통해 회원을 찾아 반환하는 메소드
 * 만약 찾은 회원이 있다면, 그대로 반환하고 없다면 회원가입 페이지로 이동시킨다.
 */
private User getUser(OAuthAttributes attributes, SocialType socialType) {

        // 리소스 서버로부터 넘겨받은 user 정보
        OAuth2UserInfo oauth2UserInfo = attributes.getOauth2UserInfo();

        // 리소스 서버로부터 넘겨받은 socialEmail
        String socialEmail = oauth2UserInfo.getEmail();

        // 리소스 서버로부터 넘겨받은 socialId
        String socialId = oauth2UserInfo.getSocialId();

        // 리소스 서버에서 넘겨받은 socialEmail 과 앱 계정의 email 이 일치하는 사용자 조회
        // 사용자가 존재하지 않으면, RegisteredUserNotFoundException 던짐
        User user = userRepository.findByEmail(socialEmail)
        .orElseThrow(RegisteredUserNotFoundException::new); // 회원가입 페이지로 보냄

        // app 계정이 존재하는 경우, 연동된 socialProfile 조회
        Optional<SocialProfile> socialProfileOptional = socialProfileRepository.findBySocialEmailAndSocialTypeWithUser(socialEmail, socialType);

        // socialProfile 연동이 안 되어있는 경우,
        if (socialProfileOptional.isEmpty()) {
        // socialProfile 자동 연동해준 뒤 홈 화면으로 이동시킴
        SocialProfile socialProfile = SocialProfile.builder()
        .user(user)
        .socialType(socialType)
        .socialId(socialId)
        .socialEmail(socialEmail)
        .build();
        socialProfileRepository.save(socialProfile); // 소셜 프로필 연동
        }

        // 소셜 로그인 시도한 socialProfile 이 이미 연동되어있는 경우,
        // 인증 성공시키고 홈 화면으로 보냄
        return user;
        }
```
</details>

③ 인증 성공 시 : OAuth2LoginSuccessHandler 호출 → Jwt Token 생성하여 클라이언트 측으로 전송
<details>
<summary>코드 보기</summary>

<!-- summary 아래 한칸 공백 두어야함 -->
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    // TODO: 아래 코드 수정 필요(LEMON 앱의 정책에 따라서)
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");
        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

            // User의 Role이 GUEST일 경우 처음 요청한 회원이므로 회원가입 페이지로 리다이렉트
            if(oAuth2User.getRole() == Role.GUEST) {
                String accessToken = jwtService.createAccessToken(oAuth2User.getEmail());
                response.addHeader(jwtService.getAccessHeader(), "Bearer " + accessToken);
                response.sendRedirect("oauth2/sign-up"); // 프론트의 회원가입 추가 정보 입력 폼으로 리다이렉트

                jwtService.sendAccessAndRefreshToken(response, accessToken, null);

            } else {
                loginSuccess(response, oAuth2User); // 로그인에 성공한 경우 access, refresh 토큰 생성
            }
        } catch (Exception e) {
            throw e;
        }

    }

    // TODO : 소셜 로그인 시에도 무조건 토큰 생성하지 말고 JWT 인증 필터처럼 RefreshToken 유/무에 따라 다르게 처리해보기
    private void loginSuccess(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {
        String accessToken = jwtService.createAccessToken(oAuth2User.getEmail());
        String refreshToken = jwtService.createRefreshToken();
        response.addHeader(jwtService.getAccessHeader(), "Bearer " + accessToken);
        response.addHeader(jwtService.getRefreshHeader(), "Bearer " + refreshToken);

        jwtService.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        jwtService.updateRefreshToken(oAuth2User.getEmail(), refreshToken);
    }
}
```
</details>


④ 인증 실패 시 : OAuth2LoginFailureHandler 호출 → 클라이언트 측으로 로그인 실패 메세지 반환
<details>
<summary>코드 보기</summary>

<!-- summary 아래 한칸 공백 두어야함 -->
```java
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("소셜 로그인 실패! 서버 로그를 확인해주세요.");
        log.info("소셜 로그인에 실패했습니다. 에러 메시지 : {}", exception.getMessage());
    }
}

```
</details>


<br/>
<br/>
<br/>
<br/>


### readme 파일에 추가 정리 예정
### 3. 이메일로 가입된 계정 찾기
### 4. 비밀번호 설정
### 5. 닉네임 중복 여부 체크
### 7. 이메일 인증코드 발송
### 8. 이메일 인증코드 검증

