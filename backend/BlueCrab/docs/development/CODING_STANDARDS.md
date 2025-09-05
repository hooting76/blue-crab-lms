# 📋 코딩 표준 가이드

## 📌 개요

BlueCrab 프로젝트의 코딩 표준 및 컨벤션을 정의한 문서입니다. 일관된 코드 스타일과 품질 유지를 위해 모든 개발자가 준수해야 합니다.

## ☕ Java 코딩 표준

### 네이밍 컨벤션

#### 클래스명
- **PascalCase** 사용
- 명사 또는 명사구 사용
- 의미가 명확해야 함

```java
// ✅ 좋은 예
public class UserController {}
public class AuthService {}
public class LoginResponse {}

// ❌ 나쁜 예  
public class usercontroller {}
public class Auth_Service {}
public class loginResp {}
```

#### 메서드명
- **camelCase** 사용
- 동사로 시작
- 의도를 명확히 표현

```java
// ✅ 좋은 예
public void authenticateUser() {}
public LoginResponse login(LoginRequest request) {}
public boolean isTokenValid(String token) {}

// ❌ 나쁜 예
public void auth() {}
public LoginResponse doLogin() {}
public boolean checkToken() {}
```

#### 변수명
- **camelCase** 사용
- 축약어 지양
- 의미 있는 이름 사용

```java
// ✅ 좋은 예
String userEmail;
Integer userId;
LoginRequest loginRequest;

// ❌ 나쁜 예
String email; // 너무 일반적
Integer id;   // 무엇의 ID인지 불명확
LoginRequest req; // 축약어 사용
```

#### 상수명
- **UPPER_SNAKE_CASE** 사용
- static final 키워드 사용

```java
// ✅ 좋은 예
public static final String JWT_SECRET_KEY = "secretKey";
public static final int MAX_LOGIN_ATTEMPTS = 3;
public static final long TOKEN_EXPIRATION_TIME = 900000L;
```

### 코드 구조

#### 패키지 구조
```
BlueCrab.com.example/
├── config/          # 설정 클래스
├── controller/      # REST 컨트롤러
├── dto/            # 데이터 전송 객체
├── entity/         # JPA 엔티티
├── exception/      # 예외 클래스
├── repository/     # 데이터 액세스 레이어
├── security/       # 보안 관련 클래스
├── service/        # 비즈니스 로직
└── util/           # 유틸리티 클래스
```

#### 클래스 구성 순서
1. static 변수
2. 인스턴스 변수
3. 생성자
4. public 메서드
5. private 메서드

```java
@Service
public class AuthService {
    // 1. static 변수
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    // 2. 인스턴스 변수
    @Autowired
    private UserTblRepository userTblRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // 3. 생성자 (필요시)
    public AuthService() {}
    
    // 4. public 메서드
    public LoginResponse authenticate(LoginRequest request) {
        // 구현
    }
    
    // 5. private 메서드
    private boolean validatePassword(String password, String encodedPassword) {
        // 구현
    }
}
```

## 🎨 코드 스타일

### 들여쓰기 및 공백
- **들여쓰기**: 4spaces 사용 (탭 사용 금지)
- **줄 길이**: 120자 이내
- **공백**: 연산자 앞뒤, 쉼표 뒤에 공백

```java
// ✅ 좋은 예
if (user != null && user.isActive()) {
    String token = jwtUtil.generateAccessToken(user.getUserEmail(), 
                                             user.getUserIdx());
    return new LoginResponse(token, "Bearer", 900L, userInfo);
}

// ❌ 나쁜 예
if(user!=null&&user.isActive()){
String token=jwtUtil.generateAccessToken(user.getUserEmail(),user.getUserIdx());
return new LoginResponse(token,"Bearer",900L,userInfo);
}
```

### 중괄호 스타일
- **Egyptian brackets** 스타일 사용
- 한 줄 if문도 중괄호 사용

```java
// ✅ 좋은 예
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}

// ❌ 나쁜 예
if (condition) 
{
    doSomething();
}
else 
{
    doSomethingElse();
}
```

## 📝 주석 및 문서화

### JavaDoc 주석
- 모든 public 클래스, 메서드에 JavaDoc 작성
- API 명세를 포함하여 작성

```java
/**
 * 사용자 로그인 인증 처리
 * 이메일과 비밀번호를 검증하여 JWT 토큰을 발급
 * 
 * @param loginRequest 로그인 요청 정보
 * @return JWT 토큰과 사용자 정보
 * @throws RuntimeException 인증 실패 시
 */
public LoginResponse authenticate(LoginRequest loginRequest) {
    // 구현
}
```

### 인라인 주석
- 복잡한 비즈니스 로직에 대한 설명
- 한글 또는 영어 사용 가능

```java
// JWT 토큰 생성 시 만료시간은 AppConfig에서 가져옴
long expiresIn = appConfig.getJwt().getAccessTokenExpiration() / 1000;

// 비밀번호 검증 실패 시 로그 기록 (보안상 상세 정보 제외)
if (!passwordEncoder.matches(password, user.getUserPw())) {
    logger.warn("비밀번호 불일치: {}", email);
    throw new RuntimeException("Invalid username or password");
}
```

## 🏗️ Spring Boot 컨벤션

### 어노테이션 사용
```java
// 컨트롤러
@RestController
@RequestMapping("/api/auth")
public class AuthController {}

// 서비스
@Service  
public class AuthService {}

// 리포지토리
@Repository
public interface UserTblRepository extends JpaRepository<UserTbl, Integer> {}

// 엔티티
@Entity
@Table(name = "USER_TBL")
public class UserTbl {}
```

### 의존성 주입
- **Field Injection** 대신 **Constructor Injection** 권장
- 단, 기존 코드와의 일관성을 위해 @Autowired 사용 가능

```java
// ✅ 권장하는 방식
@Service
public class AuthService {
    private final UserTblRepository userTblRepository;
    private final JwtUtil jwtUtil;
    
    public AuthService(UserTblRepository userTblRepository, JwtUtil jwtUtil) {
        this.userTblRepository = userTblRepository;
        this.jwtUtil = jwtUtil;
    }
}

// ✅ 기존 코드 스타일 (허용)
@Service
public class AuthService {
    @Autowired
    private UserTblRepository userTblRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
}
```

## 🔒 보안 코딩 가이드

### 비밀번호 처리
```java
// ✅ 좋은 예 - 암호화된 비밀번호만 로그
logger.info("사용자 등록: email={}", user.getUserEmail());

// ❌ 나쁜 예 - 평문 비밀번호 로그 출력 금지
logger.info("사용자 등록: email={}, password={}", email, password);
```

### SQL 인젝션 방지
- Spring Data JPA 쿼리 메서드 사용
- 동적 쿼리 시 Criteria API 또는 파라미터 바인딩 사용

```java
// ✅ 좋은 예
@Query("SELECT u FROM UserTbl u WHERE u.userEmail = :email")
Optional<UserTbl> findByUserEmail(@Param("email") String email);

// ❌ 나쁜 예 - 문자열 연결 방식
@Query("SELECT u FROM UserTbl u WHERE u.userEmail = '" + email + "'")
```

## 🧪 테스트 코드 표준

### 테스트 메서드명
- **given_when_then** 패턴 또는 한글 설명

```java
// ✅ 좋은 예
@Test
void 유효한_로그인_정보로_인증시_토큰을_반환한다() {}

@Test  
void givenValidLoginRequest_whenAuthenticate_thenReturnsToken() {}

// ❌ 나쁜 예
@Test
void testLogin() {}
```

### 테스트 구조
```java
@Test
void 유효한_로그인_정보로_인증시_토큰을_반환한다() {
    // Given
    LoginRequest loginRequest = new LoginRequest("test@example.com", "password");
    UserTbl user = createTestUser();
    
    // When  
    LoginResponse response = authService.authenticate(loginRequest);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.getAccessToken()).isNotBlank();
}
```

## 📊 성능 가이드

### 쿼리 최적화
```java
// ✅ 좋은 예 - 필요한 필드만 조회
@Query("SELECT new UserDto(u.userIdx, u.userEmail, u.userName) " +
       "FROM UserTbl u WHERE u.userEmail = :email")
Optional<UserDto> findUserSummaryByEmail(@Param("email") String email);

// ❌ 나쁜 예 - 전체 엔티티 조회
Optional<UserTbl> findByUserEmail(String email);
```

### 트랜잭션 관리
```java
// ✅ 좋은 예 - 읽기 전용 트랜잭션
@Transactional(readOnly = true)
public UserTbl findUser(String email) {}

// ✅ 좋은 예 - 쓰기 트랜잭션
@Transactional
public UserTbl saveUser(UserTbl user) {}
```

## 🔍 코드 품질 체크리스트

### 코드 리뷰 시 확인사항
- [ ] 네이밍 컨벤션 준수
- [ ] 적절한 주석 작성
- [ ] 예외 처리 적절성
- [ ] 보안 취약점 없음
- [ ] 성능 이슈 없음
- [ ] 테스트 코드 존재

### 자동화 도구
- **Checkstyle**: 코드 스타일 검사
- **SpotBugs**: 잠재적 버그 탐지  
- **SonarQube**: 코드 품질 분석

## 🚫 금지사항

### 절대 하지 말아야 할 것들
- [ ] 비밀번호, API 키 등 민감정보 하드코딩
- [ ] System.out.println() 사용 (Logger 사용 필수)
- [ ] 마법 숫자/문자열 사용 (상수로 정의)
- [ ] catch 블록에서 예외 무시
- [ ] 불필요한 주석 (코드로 설명 가능한 것)

```java
// ❌ 절대 금지
public class BadExample {
    public void badMethod() {
        System.out.println("Debug message");  // Logger 사용할것
        
        if (attempts > 3) {  // 마법 숫자 사용 금지
            // 상수로 정의: MAX_ATTEMPTS = 3
        }
        
        try {
            // some code
        } catch (Exception e) {
            // 예외 무시 금지 - 최소한 로그라도 남길것
        }
    }
}
```

---

> 💡 **참고**: 이 가이드는 프로젝트 진행에 따라 지속적으로 업데이트됩니다. 
> 새로운 컨벤션 제안이나 개선사항은 언제든 공유해주세요!