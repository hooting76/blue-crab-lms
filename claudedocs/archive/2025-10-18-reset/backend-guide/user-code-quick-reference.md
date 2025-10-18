# 사용자 코드(학번/교번) 생성 시스템 - 빠른 참조

## 🎯 코드 형식
```
2025-001-05-847
  │   │││ ││ │││
  │   │││ ││ └─┴─ 고유번호 (000~999)
  │   │││ │└──── 학과코드 (00~99)
  │   ││└─┴───── 학과코드
  │   │└───────── 학부코드 (00~99)
  │   └────────── 학생(0) / 교수(1)
  └────────────── 입학/임용년도
```

## ⚡ 빠른 사용법

### 1. 학생 등록
```java
@Autowired
private UserCodeGenerator generator;

// 2025학번, 해양과학대학(01), 양식학과(02)
String code = generator.generateUserCode(
    2025, UserType.STUDENT, 1, 2
);
// → "2025-001-02-XXX"
```

### 2. 교수 등록
```java
// 2024년 임용, 수산생명대학(02), 해양공학과(03)
String code = generator.generateUserCode(
    2024, UserType.PROFESSOR, 2, 3
);
// → "2024-102-03-XXX"
```

### 3. 현재 연도 자동 사용
```java
String code = generator.generateUserCode(
    UserType.STUDENT, 1, 5
);
// 2025년이면 → "2025-001-05-XXX"
```

### 4. 미지정 (기본값)
```java
String code = generator.generateUserCodeDefault(
    UserType.STUDENT
);
// → "2025-000-00-XXX"
```

## 📝 실제 사용 예시

```java
// UserTbl 생성 시
UserTbl student = new UserTbl();
student.setUserCode(
    generator.generateUserCode(2025, UserType.STUDENT, 1, 2)
);
student.setUserEmail("student@example.com");
student.setUserName("홍길동");
student.setUserStudent(0);  // 0=학생, 1=교수
```

## 🔍 코드 파싱
```java
UserCodeInfo info = UserCodeGenerator.parseUserCode("2025-001-05-847");
info.getYear();          // 2025
info.getUserType();      // STUDENT
info.getFacultyCode();   // 1
info.getDepartmentCode(); // 5
info.getUniqueNumber();  // 847
```

## 📋 체크리스트

등록 시 필수 작업:
- [ ] USER_CODE 생성 (UserCodeGenerator 사용)
- [ ] USER_STUDENT 설정 (0=학생, 1=교수)
- [ ] 기본 비밀번호 설정
- [ ] PROFILE_IMAGE_KEY 기본값 설정
- [ ] 지역 정보 (선택적)
- [ ] DB 중복 체크

## 📚 상세 문서
[user-code-generation-guide.md](./user-code-generation-guide.md)
