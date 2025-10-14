# 사용자 임의 등록 시나리오 가이드

## 🎯 가능한 모든 조합

### 1. 입학년도 자유 설정
```java
// 2020학번 학생
String code2020 = generator.generateUserCode(2020, UserType.STUDENT, 1, 1);
// → "2020-001-01-XXX"

// 2021학번 학생
String code2021 = generator.generateUserCode(2021, UserType.STUDENT, 1, 1);
// → "2021-001-01-XXX"

// 2025학번 학생
String code2025 = generator.generateUserCode(2025, UserType.STUDENT, 1, 1);
// → "2025-001-01-XXX"

// 현재 연도 자동 사용
String codeAuto = generator.generateUserCode(UserType.STUDENT, 1, 1);
// → "2025-001-01-XXX" (2025년 기준)
```

### 2. 학부/학과 자유 조합

#### 해양학부 (01)
```java
// 항해학과 (01)
String code = generator.generateUserCode(2025, UserType.STUDENT, 1, 1);
// → "2025-001-01-XXX"

// 해양경찰 (02)
String code = generator.generateUserCode(2025, UserType.STUDENT, 1, 2);
// → "2025-001-02-XXX"

// 해군사관 (03)
String code = generator.generateUserCode(2025, UserType.STUDENT, 1, 3);
// → "2025-001-03-XXX"
```

#### 보건학부 (02)
```java
// 간호학 (01)
String code = generator.generateUserCode(2025, UserType.STUDENT, 2, 1);
// → "2025-002-01-XXX"

// 치위생 (02)
String code = generator.generateUserCode(2025, UserType.STUDENT, 2, 2);
// → "2025-002-02-XXX"

// 약학과 (03)
String code = generator.generateUserCode(2025, UserType.STUDENT, 2, 3);
// → "2025-002-03-XXX"
```

#### 공학부 (05)
```java
// 컴퓨터공학 (01)
String code = generator.generateUserCode(2025, UserType.STUDENT, 5, 1);
// → "2025-005-01-XXX"

// 기계공학 (02)
String code = generator.generateUserCode(2025, UserType.STUDENT, 5, 2);
// → "2025-005-02-XXX"
```

### 3. 학생 vs 교수 구분
```java
// 학생
String studentCode = generator.generateUserCode(2025, UserType.STUDENT, 1, 1);
// → "2025-001-01-XXX"

// 교수
String profCode = generator.generateUserCode(2024, UserType.PROFESSOR, 1, 1);
// → "2024-101-01-XXX"
//         ↑ 1이 교수를 의미
```

### 4. 부전공 설정

#### 주전공만 있는 경우
```java
SerialCodeTable serialCode = new SerialCodeTable();
serialCode.setUserIdx(user.getUserIdx());
serialCode.setSerialCode("01");  // 해양학부
serialCode.setSerialSub("01");   // 항해학과
serialCode.setSerialCodeNd(null);  // 부전공 없음
serialCode.setSerialSubNd(null);   // 부전공 없음
```

#### 부전공 있는 경우
```java
SerialCodeTable serialCode = new SerialCodeTable();
serialCode.setUserIdx(user.getUserIdx());

// 주전공: 해양학부 항해학과
serialCode.setSerialCode("01");
serialCode.setSerialSub("01");

// 부전공: 공학부 컴퓨터공학
serialCode.setSerialCodeNd("05");
serialCode.setSerialSubNd("01");
```

## 💻 완전한 임의 등록 예시

### 시나리오 1: 2023학번 해양학부 항해학과 학생 (부전공 없음)
```java
@Service
public class UserRegistrationService {
    
    @Autowired private UserCodeGenerator userCodeGenerator;
    @Autowired private RandomAddressGenerator addressGenerator;
    @Autowired private ProfileImageKeyGenerator imageGenerator;
    
    @Transactional
    public void registerStudent_Scenario1() {
        // 1. 학번 생성: 2023학번, 해양학부(01), 항해학과(01)
        String userCode = userCodeGenerator.generateUserCode(
            2023,                          // 입학년도
            UserType.STUDENT,              // 학생
            1,                             // 해양학부
            1                              // 항해학과
        );
        // 결과: "2023-001-01-XXX"
        
        // 2. USER_TBL 생성
        UserTbl user = createUserTbl(
            userCode,
            "marine2023@bluecrab.ac.kr",
            "홍해양",
            "01012345678",
            "20050315"
        );
        userRepository.save(user);
        
        // 3. REGIST_TABLE 생성
        RegistTable regist = createRegistTable(
            user.getUserIdx(),
            userCode,
            "신규",    // 입학경로
            "재학"     // 학적상태
        );
        registRepository.save(regist);
        
        // 4. SERIAL_CODE_TABLE 생성 (부전공 없음)
        SerialCodeTable serial = new SerialCodeTable();
        serial.setUserIdx(user.getUserIdx());
        serial.setSerialCode("01");     // 해양학부
        serial.setSerialSub("01");      // 항해학과
        serial.setSerialCodeNd(null);   // 부전공 없음
        serial.setSerialSubNd(null);
        serialCodeRepository.save(serial);
    }
}
```

### 시나리오 2: 2024학번 보건학부 간호학과 학생 + 공학부 컴퓨터공학 부전공
```java
@Transactional
public void registerStudent_Scenario2() {
    // 1. 학번 생성: 2024학번, 보건학부(02), 간호학과(01)
    String userCode = userCodeGenerator.generateUserCode(
        2024,                          // 입학년도
        UserType.STUDENT,              // 학생
        2,                             // 보건학부
        1                              // 간호학과
    );
    // 결과: "2024-002-01-XXX"
    
    // 2. USER_TBL 생성
    UserTbl user = createUserTbl(
        userCode,
        "nurse2024@bluecrab.ac.kr",
        "김간호",
        "01098765432",
        "20041201"
    );
    userRepository.save(user);
    
    // 3. REGIST_TABLE 생성
    RegistTable regist = createRegistTable(
        user.getUserIdx(),
        userCode,
        "신규",
        "재학"
    );
    registRepository.save(regist);
    
    // 4. SERIAL_CODE_TABLE 생성 (부전공 있음)
    SerialCodeTable serial = new SerialCodeTable();
    serial.setUserIdx(user.getUserIdx());
    
    // 주전공: 보건학부 간호학과
    serial.setSerialCode("02");
    serial.setSerialSub("01");
    
    // 부전공: 공학부 컴퓨터공학
    serial.setSerialCodeNd("05");
    serial.setSerialSubNd("01");
    
    serialCodeRepository.save(serial);
}
```

### 시나리오 3: 2025년 임용 교수 (인문학부 경영학과)
```java
@Transactional
public void registerProfessor_Scenario3() {
    // 1. 교번 생성: 2025년, 인문학부(04), 경영학과(04)
    String userCode = userCodeGenerator.generateUserCode(
        2025,                          // 임용년도
        UserType.PROFESSOR,            // 교수
        4,                             // 인문학부
        4                              // 경영학과
    );
    // 결과: "2025-104-04-XXX"
    //              ↑ 1이 교수를 의미
    
    // 2. USER_TBL 생성
    UserTbl user = createUserTbl(
        userCode,
        "prof.kim@bluecrab.ac.kr",
        "김경영",
        "01055554444",
        "19800520"
    );
    user.setUserStudent(1);  // 교수는 1
    userRepository.save(user);
    
    // 3. REGIST_TABLE - 교수는 선택사항 (안 만들어도 됨)
    
    // 4. SERIAL_CODE_TABLE - 교수 소속 학과
    SerialCodeTable serial = new SerialCodeTable();
    serial.setUserIdx(user.getUserIdx());
    serial.setSerialCode("04");  // 인문학부
    serial.setSerialSub("04");   // 경영학과
    serialCodeRepository.save(serial);
}
```

### 시나리오 4: 2022학번 편입생 (자연과학부 물리학과 + 수학과 복수전공)
```java
@Transactional
public void registerTransferStudent_Scenario4() {
    // 1. 학번 생성: 2022학번, 자연과학부(03), 물리학과(01)
    String userCode = userCodeGenerator.generateUserCode(
        2022,                          // 입학년도
        UserType.STUDENT,              // 학생
        3,                             // 자연과학부
        1                              // 물리학과
    );
    // 결과: "2022-003-01-XXX"
    
    // 2. USER_TBL 생성
    UserTbl user = createUserTbl(
        userCode,
        "physics2022@bluecrab.ac.kr",
        "박물리",
        "01077778888",
        "20001010"
    );
    userRepository.save(user);
    
    // 3. REGIST_TABLE 생성
    RegistTable regist = createRegistTable(
        user.getUserIdx(),
        userCode,
        "편입",    // ⭐ 입학경로: 편입
        "재학"
    );
    regist.setCntTerm(4);  // 편입생은 3학년 = 4학기
    registRepository.save(regist);
    
    // 4. SERIAL_CODE_TABLE 생성 (복수전공)
    SerialCodeTable serial = new SerialCodeTable();
    serial.setUserIdx(user.getUserIdx());
    
    // 주전공: 자연과학부 물리학과
    serial.setSerialCode("03");
    serial.setSerialSub("01");
    
    // 복수전공: 자연과학부 수학과
    serial.setSerialCodeNd("03");
    serial.setSerialSubNd("02");
    
    serialCodeRepository.save(serial);
}
```

### 시나리오 5: 휴학생 (2021학번, 공학부 전자공학과)
```java
@Transactional
public void registerLeaveStudent_Scenario5() {
    // 1. 학번 생성
    String userCode = userCodeGenerator.generateUserCode(
        2021, UserType.STUDENT, 5, 3  // 공학부 전자공학
    );
    // 결과: "2021-005-03-XXX"
    
    // 2. USER_TBL 생성
    UserTbl user = createUserTbl(
        userCode,
        "electron2021@bluecrab.ac.kr",
        "이전자",
        "01099990000",
        "20030801"
    );
    userRepository.save(user);
    
    // 3. REGIST_TABLE 생성
    RegistTable regist = createRegistTable(
        user.getUserIdx(),
        userCode,
        "신규",
        "휴학"     // ⭐ 학적상태: 휴학
    );
    regist.setCntTerm(6);  // 4학년 1학기 = 7학기 이수 후 휴학
    regist.setStdRestDate("20240301");  // 휴학일자
    registRepository.save(regist);
    
    // 4. SERIAL_CODE_TABLE 생성
    SerialCodeTable serial = new SerialCodeTable();
    serial.setUserIdx(user.getUserIdx());
    serial.setSerialCode("05");
    serial.setSerialSub("03");
    serialCodeRepository.save(serial);
}
```

## 🎓 가능한 모든 학적 상태

### 입학 경로 (JOIN_PATH)
- `"신규"` - 일반 신입생
- `"편입"` - 편입생
- `"전과"` - 전과생
- `"복학"` - 복학생
- `"재입학"` - 재입학생

### 학적 상태 (STD_STAT)
- `"재학"` - 현재 재학 중
- `"휴학"` - 휴학 중
- `"졸업"` - 졸업
- `"자퇴"` - 자퇴
- `"제적"` - 제적
- `"수료"` - 수료

## 📊 대량 등록 예시 (일괄 등록)

```java
@Service
public class BulkUserRegistrationService {
    
    /**
     * 엑셀/CSV 파일에서 읽은 데이터로 대량 등록
     */
    @Transactional
    public List<UserRegistrationResult> bulkRegister(List<StudentDataDto> students) {
        List<UserRegistrationResult> results = new ArrayList<>();
        
        for (StudentDataDto data : students) {
            try {
                // 각 학생마다 임의로 설정 가능
                String userCode = userCodeGenerator.generateUserCode(
                    data.getAdmissionYear(),      // 각자 다른 입학년도
                    UserType.STUDENT,
                    data.getFacultyCode(),         // 각자 다른 학부
                    data.getDepartmentCode()       // 각자 다른 학과
                );
                
                UserTbl user = createUser(userCode, data);
                RegistTable regist = createRegist(user, data);
                SerialCodeTable serial = createSerial(user, data);
                
                // 부전공 여부도 각자 다름
                if (data.hasMinor()) {
                    serial.setSerialCodeNd(data.getMinorFacultyCode());
                    serial.setSerialSubNd(data.getMinorDepartmentCode());
                }
                
                userRepository.save(user);
                registRepository.save(regist);
                serialCodeRepository.save(serial);
                
                results.add(new UserRegistrationResult(
                    user.getUserIdx(), 
                    userCode, 
                    "success"
                ));
                
            } catch (Exception e) {
                results.add(new UserRegistrationResult(
                    null, 
                    null, 
                    "failed: " + e.getMessage()
                ));
            }
        }
        
        return results;
    }
}
```

### 엑셀 파일 예시
```
입학년도 | 이름   | 이메일                    | 학부코드 | 학과코드 | 부전공학부 | 부전공학과 | 입학경로
2023    | 홍길동 | hong@bluecrab.ac.kr      | 01      | 01      |           |           | 신규
2023    | 김철수 | kim@bluecrab.ac.kr       | 02      | 01      | 05        | 01        | 신규
2022    | 이영희 | lee@bluecrab.ac.kr       | 03      | 01      | 03        | 02        | 편입
2024    | 박민수 | park@bluecrab.ac.kr      | 04      | 04      |           |           | 신규
2021    | 최지영 | choi@bluecrab.ac.kr      | 05      | 01      | 04        | 07        | 신규
```

## ✅ 정리

### 자유롭게 설정 가능한 항목
- ✅ **입학년도**: 1900~9999 (어떤 년도든 가능)
- ✅ **학부**: 01~99 (현재 01~05 사용 중)
- ✅ **학과**: 01~99 (학부별로 다양)
- ✅ **학생/교수**: 0=학생, 1=교수
- ✅ **부전공**: 있음/없음 선택 가능
- ✅ **복수전공**: 같은 학부 내 다른 학과 가능
- ✅ **입학경로**: 신규/편입/전과/복학/재입학
- ✅ **학적상태**: 재학/휴학/졸업/자퇴/제적/수료
- ✅ **재학학기**: 0~무제한

### 자동 생성되는 항목
- 🤖 **USER_CODE 고유번호**: 000~999 랜덤
- 🤖 **주소**: 17개 시/도에서 랜덤
- 🤖 **우편번호**: 01000~63999 랜덤
- 🤖 **프로필 이미지**: 10종 기본 이미지 중 랜덤

### 따라서 가능한 시나리오
1. ✅ 2020~2025학번 혼재 등록
2. ✅ 5개 학부 24개 학과 자유 배치
3. ✅ 부전공/복수전공 자유 설정
4. ✅ 신입생/편입생/전과생 구분
5. ✅ 재학/휴학/졸업생 상태 관리
6. ✅ 학생과 교수 동시 등록
7. ✅ 엑셀 파일로 대량 일괄 등록

**결론**: 네, 입학년도, 학과, 학번, 부전공 여부 등 **모든 항목을 자유롭게 설정**하여 임의 등록이 가능합니다! 🎯
