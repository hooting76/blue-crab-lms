# 사용자 등록 시스템 - DB 구조 분석 및 자동 생성 체크리스트

## 📊 테이블 구조 분석

### 핵심 테이블 관계도

```
USER_TBL (기본 사용자 정보)
  ├─ USER_IDX (PK)
  ├─ USER_CODE (학번/교번)
  ├─ USER_EMAIL
  ├─ USER_NAME
  ├─ USER_PHONE
  ├─ USER_BIRTH
  ├─ USER_STUDENT (0: 학생, 1: 교수)
  ├─ USER_ZIP (우편번호)
  ├─ USER_FIRST_ADD (기본 주소)
  ├─ USER_LAST_ADD (상세 주소)
  └─ PROFILE_IMAGE_KEY (프로필 이미지)
       │
       ├─ REGIST_TABLE (학적 정보)
       │    ├─ USER_IDX (FK)
       │    ├─ USER_CODE (학번)
       │    ├─ JOIN_PATH (입학경로: 신규/편입 등)
       │    ├─ STD_STAT (학적상태: 재학/휴학/졸업 등)
       │    ├─ STD_REST_DATE (휴학일자)
       │    └─ CNT_TERM (학기 수)
       │
       └─ SERIAL_CODE_TABLE (전공/부전공 정보)
            ├─ USER_IDX (FK)
            ├─ SERIAL_CODE (주전공 학부코드 - 2자리)
            ├─ SERIAL_SUB (주전공 학과코드 - 2자리)
            ├─ SERIAL_CODE_ND (부전공 학부코드 - 2자리)
            └─ SERIAL_SUB_ND (부전공 학과코드 - 2자리)

FACULTY (단과대학)                    DEPARTMENT (학과)
  ├─ faculty_id (PK)                    ├─ dept_id (PK)
  ├─ faculty_code (2자리)         ◄────┤ faculty_id (FK)
  ├─ faculty_name                       ├─ dept_code (2자리)
  ├─ established_at                     ├─ dept_name
  └─ capacity                           ├─ established_at
                                        └─ capacity

PROFILE_VIEW (뷰)
  └─ USER_TBL + REGIST_TABLE + SERIAL_CODE_TABLE + FACULTY + DEPARTMENT 조인
```

## 🏫 현재 등록된 학부/학과 정보

### 단과대학 (FACULTY)
| 코드 | 단과대학명 | 정원 |
|------|-----------|------|
| 01 | 해양학부 | 410명 |
| 02 | 보건학부 | 340명 |
| 03 | 자연과학부 | 120명 |
| 04 | 인문학부 | 320명 |
| 05 | 공학부 | 320명 |

### 학과 (DEPARTMENT) - 주요 예시

#### 01 해양학부
- 01: 항해학과
- 02: 해양경찰
- 03: 해군사관
- 04: 도선학과
- 05: 해양수산학
- 06: 조선학과

#### 02 보건학부
- 01: 간호학
- 02: 치위생
- 03: 약학과
- 04: 보건정책학

#### 03 자연과학부
- 01: 물리학
- 02: 수학
- 03: 분자화학

#### 04 인문학부
- 01: 철학
- 02: 국어국문
- 03: 역사학
- 04: 경영
- 05: 경제
- 06: 정치외교
- 07: 영어영문

#### 05 공학부
- 01: 컴퓨터공학
- 02: 기계공학
- 03: 전자공학
- 04: ICT융합

## ✅ 신규 사용자 등록 시 필수 작업 체크리스트

### 1️⃣ USER_TBL 생성 (필수)
```sql
INSERT INTO USER_TBL (
    USER_EMAIL,
    USER_PW,
    USER_NAME,
    USER_CODE,          -- ⚠️ UserCodeGenerator로 생성
    USER_PHONE,
    USER_BIRTH,
    USER_STUDENT,       -- 0: 학생, 1: 교수
    USER_ZIP,           -- ⚠️ 자동 생성 권장
    USER_FIRST_ADD,     -- ⚠️ 자동 생성 권장
    USER_LAST_ADD,      -- ⚠️ 자동 생성 권장 (null 가능)
    PROFILE_IMAGE_KEY,  -- ⚠️ 기본값 설정 권장
    USER_REG,
    USER_REG_IP
) VALUES (...);
```

**자동 생성 필요 항목**:
- ✅ `USER_CODE`: UserCodeGenerator 사용
- ⚠️ `USER_ZIP`: 랜덤 우편번호 생성
- ⚠️ `USER_FIRST_ADD`: 랜덤 주소 생성
- ⚠️ `USER_LAST_ADD`: null 또는 랜덤
- ⚠️ `PROFILE_IMAGE_KEY`: 기본 프로필 이미지 설정

### 2️⃣ REGIST_TABLE 생성 (학생인 경우 필수)
```sql
INSERT INTO REGIST_TABLE (
    USER_IDX,
    USER_CODE,          -- USER_TBL.USER_CODE와 동일
    JOIN_PATH,          -- '신규', '편입', '전과' 등
    STD_STAT,           -- '재학', '휴학', '졸업' 등
    CNT_TERM,           -- 재학 학기 수 (기본 0)
    ADMIN_NAME,         -- 등록한 관리자 이름
    ADMIN_REG,          -- 등록 일시
    ADMIN_IP            -- 관리자 IP
) VALUES (...);
```

**기본값**:
- `JOIN_PATH`: '신규' (기본값)
- `STD_STAT`: '재학' (기본값)
- `CNT_TERM`: 0 (신입생)

### 3️⃣ SERIAL_CODE_TABLE 생성 (전공 배정 시 필수)
```sql
INSERT INTO SERIAL_CODE_TABLE (
    USER_IDX,
    SERIAL_CODE,        -- 주전공 학부코드 (2자리)
    SERIAL_SUB,         -- 주전공 학과코드 (2자리)
    SERIAL_CODE_ND,     -- 부전공 학부코드 (선택)
    SERIAL_SUB_ND,      -- 부전공 학과코드 (선택)
    SERIAL_REG          -- 등록일
) VALUES (...);
```

**중요**: 
- USER_CODE 생성 시 사용한 학부/학과 코드와 동일하게 설정
- 부전공은 선택사항 (null 가능)

## 📋 현재 DB 데이터 현황

### USER_TBL
- 총 14명
- 프로필 이미지 있음: 1명만 (`202500101000_20250925173216.jpg`)
- 우편번호 있음: 전체 (하지만 대부분 의미없는 값)
- 주소 있음: 2명만

### REGIST_TABLE
- 데이터 있음: 10건
- 대부분 USER_CODE가 '0' (잘못된 데이터)
- 올바른 학번: 1건만 (`240105045`)

### SERIAL_CODE_TABLE
- 데이터 있음: 1건만
- USER_IDX 15번 사용자만 등록됨
  - 주전공: 01학부 01학과 (해양학부 항해학과)
  - 부전공: 05학부 03학과 (공학부 전자공학)

## 🛠️ 구현해야 할 자동 생성 유틸리티

### 1. UserCodeGenerator ✅ (이미 완료)
```java
String userCode = userCodeGenerator.generateUserCode(
    2025,                                // 년도
    UserCodeGenerator.UserType.STUDENT,  // 학생/교수
    1,                                   // 학부코드
    1                                    // 학과코드
);
// 결과: "2025-001-01-847"
```

### 2. AddressGenerator ⚠️ (구현 필요)
```java
public class RandomAddressGenerator {
    
    private static final String[] CITIES = {
        "서울특별시", "부산광역시", "인천광역시", "대구광역시",
        "대전광역시", "광주광역시", "울산광역시", "세종특별자치시"
    };
    
    private static final Map<String, String[]> DISTRICTS = Map.of(
        "서울특별시", new String[]{"강남구", "서초구", "송파구", "강동구", "중구", "용산구"},
        "부산광역시", new String[]{"해운대구", "수영구", "남구", "동래구", "연제구"},
        "인천광역시", new String[]{"남동구", "연수구", "부평구", "계양구", "서구"}
        // ... 더 추가
    );
    
    public static AddressInfo generateRandomAddress() {
        Random random = new Random();
        String city = CITIES[random.nextInt(CITIES.length)];
        String[] cityDistricts = DISTRICTS.getOrDefault(city, new String[]{"중구"});
        String district = cityDistricts[random.nextInt(cityDistricts.length)];
        String zipCode = String.format("%05d", random.nextInt(100000));
        String mainAddress = city + " " + district + " " + generateRandomStreetName();
        
        return new AddressInfo(zipCode, mainAddress, null);
    }
    
    private static String generateRandomStreetName() {
        String[] streets = {"중앙로", "시청로", "역삼로", "테헤란로", "강남대로"};
        Random random = new Random();
        int number = random.nextInt(500) + 1;
        return streets[random.nextInt(streets.length)] + " " + number;
    }
}
```

### 3. ProfileImageKeyGenerator ⚠️ (구현 필요)
```java
public class ProfileImageKeyGenerator {
    
    /**
     * 기본 프로필 이미지 키 반환
     * MinIO에 미리 업로드된 기본 이미지 중 랜덤 선택
     */
    public static String getDefaultProfileImageKey() {
        // 기본 프로필 이미지 목록
        String[] defaultImages = {
            "default/profile_default_1.jpg",
            "default/profile_default_2.jpg",
            "default/profile_default_3.jpg",
            "default/profile_default_4.jpg",
            "default/profile_default_5.jpg"
        };
        
        Random random = new Random();
        return defaultImages[random.nextInt(defaultImages.length)];
    }
    
    /**
     * 사용자 코드 기반 프로필 이미지 키 생성
     * 실제 이미지가 업로드되면 사용
     */
    public static String generateProfileImageKey(String userCode, String extension) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );
        return userCode + "_" + timestamp + "." + extension;
    }
}
```

## 💻 완전한 사용자 등록 서비스 예시

```java
@Service
public class UserRegistrationService {
    
    @Autowired
    private UserCodeGenerator userCodeGenerator;
    
    @Autowired
    private UserTblRepository userTblRepository;
    
    @Autowired
    private RegistTableRepository registTableRepository;
    
    @Autowired
    private SerialCodeTableRepository serialCodeTableRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 신규 학생 등록 (완전 자동화)
     */
    @Transactional
    public UserRegistrationResult registerStudent(StudentRegistrationDto dto) {
        
        // 1. USER_CODE 생성
        String userCode = userCodeGenerator.generateUserCode(
            dto.getAdmissionYear(),
            UserCodeGenerator.UserType.STUDENT,
            dto.getFacultyCode(),
            dto.getDepartmentCode()
        );
        
        // 2. 랜덤 주소 생성
        AddressInfo address = RandomAddressGenerator.generateRandomAddress();
        
        // 3. 기본 프로필 이미지 키 설정
        String profileImageKey = ProfileImageKeyGenerator.getDefaultProfileImageKey();
        
        // 4. USER_TBL 저장
        UserTbl user = new UserTbl();
        user.setUserCode(userCode);
        user.setUserEmail(dto.getEmail());
        user.setUserPw(passwordEncoder.encode(dto.getInitialPassword())); // 초기 비밀번호
        user.setUserName(dto.getName());
        user.setUserPhone(dto.getPhone());
        user.setUserBirth(dto.getBirth());
        user.setUserStudent(0); // 학생
        user.setUserZip(Integer.parseInt(address.getZipCode()));
        user.setUserFirstAdd(address.getMainAddress());
        user.setUserLastAdd(address.getDetailAddress());
        user.setProfileImageKey(profileImageKey);
        user.setUserReg(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        UserTbl savedUser = userTblRepository.save(user);
        
        // 5. REGIST_TABLE 저장
        RegistTable regist = new RegistTable();
        regist.setUserIdx(savedUser.getUserIdx());
        regist.setUserCode(userCode);
        regist.setJoinPath(dto.getJoinPath() != null ? dto.getJoinPath() : "신규");
        regist.setStdStat("재학");
        regist.setCntTerm(0);
        regist.setAdminReg(LocalDateTime.now());
        
        registTableRepository.save(regist);
        
        // 6. SERIAL_CODE_TABLE 저장 (전공 정보)
        SerialCodeTable serialCode = new SerialCodeTable();
        serialCode.setUserIdx(savedUser.getUserIdx());
        serialCode.setSerialCode(String.format("%02d", dto.getFacultyCode()));
        serialCode.setSerialSub(String.format("%02d", dto.getDepartmentCode()));
        serialCode.setSerialReg(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        // 부전공이 있는 경우
        if (dto.getMinorFacultyCode() != null && dto.getMinorDepartmentCode() != null) {
            serialCode.setSerialCodeNd(String.format("%02d", dto.getMinorFacultyCode()));
            serialCode.setSerialSubNd(String.format("%02d", dto.getMinorDepartmentCode()));
            serialCode.setSerialRegNd(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        }
        
        serialCodeTableRepository.save(serialCode);
        
        return new UserRegistrationResult(
            savedUser.getUserIdx(),
            userCode,
            "학생 등록이 완료되었습니다."
        );
    }
}
```

## 🔍 검증 사항

### 등록 후 확인해야 할 사항
1. ✅ USER_TBL에 데이터 생성됨
2. ✅ REGIST_TABLE에 학적 정보 생성됨
3. ✅ SERIAL_CODE_TABLE에 전공 정보 생성됨
4. ✅ PROFILE_VIEW에서 조회 가능
5. ✅ 학번(USER_CODE)이 올바른 형식
6. ✅ 주소, 프로필 이미지 키가 설정됨

### 쿼리로 확인
```sql
-- 특정 학생 등록 확인
SELECT 
    u.USER_IDX,
    u.USER_CODE,
    u.USER_NAME,
    u.USER_ZIP,
    u.USER_FIRST_ADD,
    u.PROFILE_IMAGE_KEY,
    r.STD_STAT,
    s.SERIAL_CODE,
    s.SERIAL_SUB
FROM USER_TBL u
LEFT JOIN REGIST_TABLE r ON u.USER_IDX = r.USER_IDX
LEFT JOIN SERIAL_CODE_TABLE s ON u.USER_IDX = s.USER_IDX
WHERE u.USER_CODE = '2025-001-01-847';
```

## 📌 다음 단계

1. ✅ UserCodeGenerator - 완료
2. ⚠️ RandomAddressGenerator - 구현 필요
3. ⚠️ ProfileImageKeyGenerator - 구현 필요
4. ⚠️ UserRegistrationService - 통합 서비스 구현 필요
5. ⚠️ MinIO에 기본 프로필 이미지 업로드
6. ⚠️ 관리자 페이지 UI 구현
7. ⚠️ 엑셀 일괄 등록 기능

## 🚨 현재 DB 문제점

1. **REGIST_TABLE**: USER_CODE가 대부분 '0' - 기존 데이터 정리 필요
2. **SERIAL_CODE_TABLE**: 1건만 존재 - 기존 사용자 전공 정보 누락
3. **프로필 이미지**: 1명만 있음 - 나머지 기본값 설정 필요
4. **주소**: 대부분 의미없는 우편번호 - 정리 필요
