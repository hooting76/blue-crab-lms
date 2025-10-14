# 사용자 코드(학번/교번) 자동 생성 시스템

## 📋 개요

관리자가 합격자 명단이나 신규 교직원을 등록할 때 자동으로 학번/교번을 생성하는 시스템입니다.

## 🎯 코드 형식

```
YYYY-ABC-DE-FGH
```

| 부분 | 설명 | 범위 | 예시 |
|------|------|------|------|
| **YYYY** | 입학년도/임용년도 | 1900~9999 | 2025 |
| **A** | 학생/교수 구분 | 0: 학생<br>1: 교수 | 0, 1 |
| **BC** | 학부/단과대학 코드 | 00~99 | 01 (해양대학) |
| **DE** | 학과 코드 | 00~99 | 05 (해양생물학과) |
| **FGH** | 고유 난수 | 000~999 | 847 |

## 📝 코드 예시

### 학생 코드
```
2025-001-05-847
└─┬─┘ │││ ││ │││
  │   │││ ││ │└─ 고유번호 (자동생성)
  │   │││ ││ └── 고유번호
  │   │││ │└──── 고유번호
  │   │││ └───── 학과: 05
  │   ││└──────── 학과: 05  
  │   │└───────── 학부: 01
  │   └────────── 학생: 0
  └────────────── 입학년도: 2025
```

**의미**: 2025학번, 01학부, 05학과 소속 학생

### 교수 코드
```
2024-110-02-321
└─┬─┘ │││ ││ │││
  │   │││ ││ └─┴─ 고유번호: 321
  │   │││ │└──── 학과: 02
  │   ││└─┴───── 학과: 02
  │   │└───────── 학부: 10
  │   └────────── 교수: 1
  └────────────── 임용년도: 2024
```

**의미**: 2024년 임용, 10학부, 02학과 소속 교수

## 💻 사용 방법

### 1. 기본 사용 (년도, 학부, 학과 지정)

```java
@Autowired
private UserCodeGenerator userCodeGenerator;

// 2025학번 학생, 01학부, 05학과
String studentCode = userCodeGenerator.generateUserCode(
    2025,                                   // 입학년도
    UserCodeGenerator.UserType.STUDENT,    // 학생
    1,                                      // 학부 코드
    5                                       // 학과 코드
);
// 결과 예: "2025-001-05-847"

// 2024년 임용 교수, 10학부, 02학과
String profCode = userCodeGenerator.generateUserCode(
    2024,                                    // 임용년도
    UserCodeGenerator.UserType.PROFESSOR,   // 교수
    10,                                      // 학부 코드
    2                                        // 학과 코드
);
// 결과 예: "2024-110-02-321"
```

### 2. 현재 연도로 자동 생성

```java
// 현재 연도 자동 적용
String code = userCodeGenerator.generateUserCode(
    UserCodeGenerator.UserType.STUDENT,    // 학생
    3,                                      // 학부 코드
    7                                       // 학과 코드
);
// 2025년이면 결과 예: "2025-003-07-492"
```

### 3. 미지정 학부/학과 (기본값 00)

```java
// 학부/학과 미지정으로 생성
String code = userCodeGenerator.generateUserCodeDefault(
    2025,                                   // 년도
    UserCodeGenerator.UserType.STUDENT     // 학생
);
// 결과 예: "2025-000-00-156"
```

### 4. 현재 연도 + 미지정 학부/학과

```java
// 가장 간단한 사용 (현재 연도, 학부/학과 미지정)
String code = userCodeGenerator.generateUserCodeDefault(
    UserCodeGenerator.UserType.PROFESSOR   // 교수
);
// 2025년이면 결과 예: "2025-100-00-789"
```

### 5. 코드 파싱 (분석/검증)

```java
String userCode = "2025-001-05-847";

UserCodeInfo info = UserCodeGenerator.parseUserCode(userCode);

System.out.println("입학/임용 년도: " + info.getYear());           // 2025
System.out.println("사용자 유형: " + info.getUserType());           // STUDENT
System.out.println("학부 코드: " + info.getFacultyCode());         // 1
System.out.println("학과 코드: " + info.getDepartmentCode());      // 5
System.out.println("고유 번호: " + info.getUniqueNumber());        // 847
```

## 🏫 학부/학과 코드 예시 (해양대학교 기준)

### 단과대학(학부) 코드
| 코드 | 단과대학명 |
|------|-----------|
| 00 | 미지정 |
| 01 | 해양과학대학 |
| 02 | 수산생명과학대학 |
| 03 | 공과대학 |
| 04 | 국제해양대학 |
| ... | ... |

### 학과 코드 (예시)
| 코드 | 학과명 |
|------|--------|
| 00 | 미지정 |
| 01 | 해양생물학과 |
| 02 | 양식학과 |
| 03 | 해양공학과 |
| 04 | 선박해양공학과 |
| 05 | 해양환경학과 |
| ... | ... |

## 🔧 관리자 페이지 구현 예시

### 학생 일괄 등록 API

```java
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    
    @Autowired
    private UserCodeGenerator userCodeGenerator;
    
    @Autowired
    private UserTblService userTblService;
    
    /**
     * 학생 일괄 등록
     */
    @PostMapping("/students/bulk")
    public ResponseEntity<ApiResponse<List<UserTbl>>> bulkRegisterStudents(
            @RequestBody List<StudentRegistrationDto> students) {
        
        List<UserTbl> createdUsers = new ArrayList<>();
        
        for (StudentRegistrationDto dto : students) {
            // 학번 자동 생성
            String userCode = userCodeGenerator.generateUserCode(
                dto.getYear(),          // 입학년도
                UserType.STUDENT,       // 학생
                dto.getFacultyCode(),   // 학부
                dto.getDepartmentCode() // 학과
            );
            
            // 사용자 생성
            UserTbl user = new UserTbl();
            user.setUserCode(userCode);
            user.setUserEmail(dto.getEmail());
            user.setUserName(dto.getName());
            user.setUserPhone(dto.getPhone());
            user.setUserBirth(dto.getBirth());
            user.setUserStudent(0);  // 학생
            
            // 기본 비밀번호 설정 (생년월일 등)
            user.setUserPw(passwordEncoder.encode(dto.getBirth()));
            
            // 저장
            UserTbl created = userTblService.createUser(user);
            createdUsers.add(created);
        }
        
        return ResponseEntity.ok(
            ApiResponse.success(
                createdUsers.size() + "명의 학생이 등록되었습니다.", 
                createdUsers
            )
        );
    }
    
    /**
     * 교수 등록
     */
    @PostMapping("/professors")
    public ResponseEntity<ApiResponse<UserTbl>> registerProfessor(
            @RequestBody ProfessorRegistrationDto dto) {
        
        // 교번 자동 생성
        String userCode = userCodeGenerator.generateUserCode(
            dto.getYear(),          // 임용년도
            UserType.PROFESSOR,     // 교수
            dto.getFacultyCode(),   // 소속 학부
            dto.getDepartmentCode() // 소속 학과
        );
        
        UserTbl professor = new UserTbl();
        professor.setUserCode(userCode);
        professor.setUserEmail(dto.getEmail());
        professor.setUserName(dto.getName());
        professor.setUserPhone(dto.getPhone());
        professor.setUserBirth(dto.getBirth());
        professor.setUserStudent(1);  // 교수
        
        UserTbl created = userTblService.createUser(professor);
        
        return ResponseEntity.ok(
            ApiResponse.success("교수가 등록되었습니다.", created)
        );
    }
}
```

## 📊 프로필 이미지 처리

### 기본 프로필 이미지 설정

```java
// 프로필 이미지가 없는 경우 기본 이미지 사용
if (user.getProfileImageKey() == null || user.getProfileImageKey().isEmpty()) {
    // MinIO에 업로드된 기본 프로필 이미지 키
    user.setProfileImageKey("default/profile_default.jpg");
}
```

### 지역 정보 랜덤 설정

```java
import java.util.Random;

public class AddressGenerator {
    
    private static final String[] CITIES = {
        "서울특별시", "부산광역시", "인천광역시", "대구광역시", 
        "대전광역시", "광주광역시", "울산광역시", "세종특별자치시"
    };
    
    private static final String[] DISTRICTS = {
        "강남구", "서초구", "송파구", "강동구", "중구", "용산구",
        "성동구", "광진구", "동대문구", "중랑구", "성북구", "강북구"
    };
    
    public static String generateRandomAddress() {
        Random random = new Random();
        String city = CITIES[random.nextInt(CITIES.length)];
        String district = DISTRICTS[random.nextInt(DISTRICTS.length)];
        return city + " " + district;
    }
    
    public static String generateRandomZipCode() {
        Random random = new Random();
        return String.format("%05d", random.nextInt(100000));
    }
}

// 사용 예시
user.setUserFirstAdd(AddressGenerator.generateRandomAddress());
user.setUserZip(Integer.parseInt(AddressGenerator.generateRandomZipCode()));
```

## ✅ 테스트

테스트 실행:
```bash
cd backend/BlueCrab
mvn test -Dtest=UserCodeGeneratorTest
```

## 📌 주의사항

1. **고유성 보장**: 고유 난수(000~999)는 랜덤 생성이므로 이론적으로 중복 가능
   - DB에 저장 전 중복 체크 필요
   - 중복 발생 시 재생성 로직 필요

2. **학부/학과 코드 관리**: 
   - 별도 테이블로 관리 권장
   - 코드 변경 시 기존 학생 코드는 유지

3. **보안**:
   - 생성된 코드는 민감정보이므로 로그 출력 주의
   - 초기 비밀번호는 안전하게 전달

## 🔜 향후 개선 사항

- [ ] DB 중복 체크 자동화
- [ ] 학부/학과 코드 관리 테이블 생성
- [ ] 대량 등록 시 트랜잭션 처리
- [ ] 등록 이력 로그 기록
- [ ] 엑셀 파일 업로드를 통한 일괄 등록
- [ ] 등록 완료 시 이메일 자동 발송
