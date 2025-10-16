# DTO 리팩토링 Phase 1: 현황 분석 보고서

**작성일**: 2025-10-15
**분석 범위**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/dto`

---

## 📊 현황 요약

### 전체 통계
- **총 DTO 개수**: 61개
- **현재 디렉토리 구조**:
  - 루트 (`dto/`): 52개
  - 하위 폴더 (`dto/Lecture/`): 9개
- **와일드카드 import 사용**: 7개 파일

### 주요 문제점
1. ✅ **중복 DTO 존재** (2쌍, 4개 파일)
   - `AttendanceDto` (루트 vs Lecture)
   - `GradeDto` (루트 vs Lecture)

2. ✅ **와일드카드 import 남용** (7개 파일)
   ```
   AdminFacilityReservationController.java
   AdminFacilityReservationService.java
   FacilityReservationService.java
   FcmTokenController.java
   FcmTokenService.java
   ReadingRoomController.java
   ReadingRoomService.java
   ```

3. ✅ **도메인별 응집력 부족**
   - 모든 DTO가 한 폴더에 평탄하게 배치
   - 기능 간 관계 파악 어려움

---

## 🔍 중복 DTO 상세 분석

### 1. AttendanceDto (100% 동일)

**루트 버전**: `dto/AttendanceDto.java`
- 패키지: `BlueCrab.com.example.dto`
- 작성자 정보 없음
- 라인 수: 83행

**Lecture 버전**: `dto/Lecture/AttendanceDto.java`
- 패키지: `BlueCrab.com.example.dto.Lecture`
- 작성자: 성태준
- 라인 수: 85행

**차이점**: 작성자 주석만 있음, 코드 내용 100% 동일

**사용처**:
- `dto/Lecture/EnrollmentDto.java:29` - `List<AttendanceDto>` 필드 (같은 패키지 참조)
- ❌ 루트 버전은 **사용되지 않음** (Dead Code)

**권장사항**:
- ✅ **루트 버전 삭제** (`dto/AttendanceDto.java`)
- ✅ Lecture 버전만 유지
- ✅ 영향 범위: 없음 (명시적 import 없음)

---

### 2. GradeDto (100% 동일)

**루트 버전**: `dto/GradeDto.java`
- 패키지: `BlueCrab.com.example.dto`
- 작성자 정보 없음
- 라인 수: 97행

**Lecture 버전**: `dto/Lecture/GradeDto.java`
- 패키지: `BlueCrab.com.example.dto.Lecture`
- 작성자: 성태준
- 라인 수: 99행

**차이점**: 작성자 주석만 있음, 코드 내용 100% 동일

**사용처**:
- `dto/Lecture/EnrollmentDto.java:30` - `GradeDto` 필드 (같은 패키지 참조)
- ❌ 루트 버전은 **사용되지 않음** (Dead Code)

**권장사항**:
- ✅ **루트 버전 삭제** (`dto/GradeDto.java`)
- ✅ Lecture 버전만 유지
- ✅ 영향 범위: 없음 (명시적 import 없음)

---

## 🗂️ 도메인별 DTO 분류

### 🔐 Auth (인증/인가) - 9개
```
LoginRequest.java
LoginResponse.java
RefreshTokenRequest.java
LogoutRequest.java
AuthResponse.java
AuthCodeVerifyRequest.java
ChangePasswordRequest.java
FindIdRequest.java
FindIdResponse.java
```
**연관 컨트롤러**: `AuthController`, `FindIdController`, `PasswordResetController`, `MailAuthCheckController`

---

### 👤 Admin (관리자) - 5개
```
AdminLoginRequest.java
AdminLoginResponse.java
AdminRefreshRequest.java
AdminApproveRequestDto.java
AdminRejectRequestDto.java
```
**연관 컨트롤러**: `AdminController`, `AdminAuthTokenController`, `AdminFacilityReservationController`

---

### 🔑 Password Reset (비밀번호 재설정) - 4개
```
PasswordResetIdentityRequest.java
PasswordResetIdentityResponse.java
PasswordResetCodeVerifyRequest.java
PasswordResetCodeVerifyResponse.java
```
**연관 컨트롤러**: `PasswordResetController`

---

### 🏢 Facility (시설 예약) - 6개
```
FacilityDto.java
FacilityAvailabilityDto.java
DailyScheduleDto.java
ReservationDto.java
ReservationCreateRequestDto.java
```
**연관 컨트롤러**: `FacilityController`, `FacilityReservationController`, `AdminFacilityReservationController`
**연관 서비스**: `FacilityService`, `FacilityReservationService`, `AdminFacilityReservationService`

---

### 📚 Reading Room (열람실) - 5개
```
ReadingRoomStatusDto.java
ReadingSeatDto.java
SeatReserveRequestDto.java
SeatReserveResponseDto.java
SeatCheckoutRequestDto.java
SeatCheckoutResponseDto.java
```
**연관 컨트롤러**: `ReadingRoomController`
**연관 서비스**: `ReadingRoomService`

---

### 🔔 FCM & Push (알림) - 11개
```
FcmRegisterRequest.java
FcmRegisterResponse.java
FcmUnregisterRequest.java
FcmSendRequest.java
FcmSendResponse.java
FcmBatchSendRequest.java
FcmBatchSendResponse.java
FcmBroadcastRequest.java
FcmBroadcastResponse.java
FcmStatsResponse.java
PushNotificationRequest.java
TopicPushNotificationRequest.java
```
**연관 컨트롤러**: `FcmTokenController`, `PushNotificationController`
**연관 서비스**: `FcmTokenService`, `FcmSessionService`, `FirebasePushService`

---

### 📋 Board (게시판) - 1개
```
AttachmentLinkRequest.java
```
**연관 컨트롤러**: `BoardController`, `BoardCreateController`, `BoardUpdateController`, `BoardAttachmentUploadController`, `BoardAttachmentDeleteController`, `BoardAttachmentDownloadController`

---

### 🎓 Lecture (강의) - 9개
```
dto/Lecture/LectureDto.java
dto/Lecture/LectureDetailDto.java
dto/Lecture/LectureCreateRequest.java
dto/Lecture/LectureUpdateRequest.java
dto/Lecture/EnrollmentDto.java
dto/Lecture/EnrollmentCreateRequest.java
dto/Lecture/AssignmentDto.java
dto/Lecture/AssignmentSubmissionDto.java
dto/Lecture/AssignmentStatisticsDto.java
dto/Lecture/AttendanceDto.java ✅
dto/Lecture/GradeDto.java ✅
```
**상태**: ✅ 이미 하위 패키지로 구조화됨
**권장사항**: 루트 중복 제거만 필요

---

### 📜 Registry (학적/증명서) - 4개
```
RegistryRequestDTO.java
RegistryResponseDTO.java
CertIssueRequestDTO.java
CertIssueResponseDTO.java
```
**연관 컨트롤러**: `RegistryController`
**연관 서비스**: `RegistryService`, `CertIssueService`

---

### 👤 Profile (프로필) - 1개
```
ImageRequest.java
```
**연관 컨트롤러**: `ProfileController`
**연관 서비스**: `ProfileService`

---

### 🌐 Common (공통) - 1개
```
ApiResponse.java
```
**사용처**: 프로젝트 전역 (모든 컨트롤러에서 사용)

---

## 🎯 제안된 디렉토리 구조

```
dto/
├── common/
│   └── ApiResponse.java                    # 전역 공통 응답
│
├── admin/
│   ├── AdminLoginRequest.java
│   ├── AdminLoginResponse.java
│   ├── AdminRefreshRequest.java
│   ├── AdminApproveRequestDto.java
│   └── AdminRejectRequestDto.java
│
├── auth/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RefreshTokenRequest.java
│   ├── LogoutRequest.java
│   ├── AuthResponse.java
│   ├── AuthCodeVerifyRequest.java
│   ├── ChangePasswordRequest.java
│   ├── FindIdRequest.java
│   ├── FindIdResponse.java
│   ├── PasswordResetIdentityRequest.java
│   ├── PasswordResetIdentityResponse.java
│   ├── PasswordResetCodeVerifyRequest.java
│   └── PasswordResetCodeVerifyResponse.java
│
├── facility/
│   ├── FacilityDto.java
│   ├── FacilityAvailabilityDto.java
│   ├── DailyScheduleDto.java
│   ├── ReservationDto.java
│   └── ReservationCreateRequestDto.java
│
├── readingroom/
│   ├── ReadingRoomStatusDto.java
│   ├── ReadingSeatDto.java
│   ├── SeatReserveRequestDto.java
│   ├── SeatReserveResponseDto.java
│   ├── SeatCheckoutRequestDto.java
│   └── SeatCheckoutResponseDto.java
│
├── fcm/
│   ├── FcmRegisterRequest.java
│   ├── FcmRegisterResponse.java
│   ├── FcmUnregisterRequest.java
│   ├── FcmSendRequest.java
│   ├── FcmSendResponse.java
│   ├── FcmBatchSendRequest.java
│   ├── FcmBatchSendResponse.java
│   ├── FcmBroadcastRequest.java
│   ├── FcmBroadcastResponse.java
│   └── FcmStatsResponse.java
│
├── push/
│   ├── PushNotificationRequest.java
│   └── TopicPushNotificationRequest.java
│
├── board/
│   └── AttachmentLinkRequest.java
│
├── lecture/                                # 기존 유지
│   ├── LectureDto.java
│   ├── LectureDetailDto.java
│   ├── LectureCreateRequest.java
│   ├── LectureUpdateRequest.java
│   ├── EnrollmentDto.java
│   ├── EnrollmentCreateRequest.java
│   ├── AssignmentDto.java
│   ├── AssignmentSubmissionDto.java
│   ├── AssignmentStatisticsDto.java
│   ├── AttendanceDto.java
│   └── GradeDto.java
│
├── registry/
│   ├── RegistryRequestDTO.java
│   ├── RegistryResponseDTO.java
│   ├── CertIssueRequestDTO.java
│   └── CertIssueResponseDTO.java
│
└── profile/
    └── ImageRequest.java
```

---

## 🚀 Phase 2 실행 계획

### Step 1: Quick Win - Lecture 중복 제거 (리스크: 낮음)
**목표**: Dead Code 제거, 빌드 안정성 확보

**작업 내역**:
1. 삭제 대상 파일 (2개):
   - `dto/AttendanceDto.java`
   - `dto/GradeDto.java`

2. 검증 단계:
   ```bash
   # 컴파일 검증
   mvn clean compile -pl backend/BlueCrab

   # 테스트 검증
   mvn test -pl backend/BlueCrab

   # 중복 제거 확인
   find backend/BlueCrab/src/main/java -name "AttendanceDto.java"
   find backend/BlueCrab/src/main/java -name "GradeDto.java"
   ```

3. 예상 결과:
   - ✅ 컴파일 성공 (사용처 없음)
   - ✅ 테스트 통과 (영향 없음)
   - ✅ Dead Code 제거

**예상 소요 시간**: 10분

---

### Step 2: 와일드카드 Import 제거 (리스크: 낮음)
**목표**: 명시적 import로 전환, 의존성 명확화

**대상 파일** (7개):
```
AdminFacilityReservationController.java
AdminFacilityReservationService.java
FacilityReservationService.java
FcmTokenController.java
FcmTokenService.java
ReadingRoomController.java
ReadingRoomService.java
```

**작업 방법**:
1. 각 파일에서 실제 사용 중인 DTO 찾기
2. `import BlueCrab.com.example.dto.*;` → 명시적 import로 교체
3. 컴파일 검증

**검증 명령**:
```bash
# 와일드카드 import 남은 파일 확인
grep -r "import BlueCrab\.com\.example\.dto\.\*;" backend/BlueCrab/src/main/java --include="*.java"
```

**예상 소요 시간**: 20분

---

### Step 3: 도메인별 DTO 이동 - 우선순위 도메인

#### 3-1. FCM 도메인 (우선순위: 높음)
**이유**: 현재 활발히 작업 중 (git status 참고)

**작업**:
1. 새 패키지 생성: `dto/fcm/`, `dto/push/`
2. DTO 이동 (13개)
3. FcmTokenController, FcmTokenService import 수정
4. 컴파일 & 테스트

**예상 소요 시간**: 30분

---

#### 3-2. Admin 도메인 (우선순위: 중간)
**작업**:
1. 새 패키지 생성: `dto/admin/`
2. DTO 이동 (5개)
3. AdminAuthTokenController, AdminFacilityReservationController import 수정
4. 컴파일 & 테스트

**예상 소요 시간**: 20분

---

#### 3-3. Auth 도메인 (우선순위: 중간)
**작업**:
1. 새 패키지 생성: `dto/auth/`
2. DTO 이동 (13개: auth 9개 + password reset 4개)
3. AuthController, PasswordResetController, FindIdController import 수정
4. 컴파일 & 테스트

**예상 소요 시간**: 40분

---

#### 3-4. Facility & ReadingRoom (우선순위: 중간)
**작업**:
1. 새 패키지 생성: `dto/facility/`, `dto/readingroom/`
2. DTO 이동 (11개)
3. 관련 컨트롤러/서비스 import 수정
4. 컴파일 & 테스트

**예상 소요 시간**: 40분

---

#### 3-5. 나머지 도메인 (우선순위: 낮음)
**작업**:
1. Registry, Board, Profile, Common 패키지 생성 및 이동
2. 관련 컨트롤러/서비스 import 수정
3. 최종 검증

**예상 소요 시간**: 30분

---

## 📋 검증 체크리스트

각 단계마다 다음 검증 수행:

### 컴파일 검증
```bash
cd backend/BlueCrab
mvn clean compile -pl backend/BlueCrab
```

### 테스트 검증
```bash
mvn test -pl backend/BlueCrab
```

### 와일드카드 Import 제거 확인
```bash
grep -r "import BlueCrab\.com\.example\.dto\.\*;" src/main/java --include="*.java"
# 결과: 없어야 함
```

### 중복 DTO 제거 확인
```bash
find src/main/java -name "AttendanceDto.java" | wc -l  # 결과: 1
find src/main/java -name "GradeDto.java" | wc -l       # 결과: 1
```

### 패키지 구조 확인
```bash
find src/main/java/BlueCrab/com/example/dto -type d
```

---

## ⚠️ 리스크 및 완화 전략

### 리스크 1: 와일드카드 Import로 인한 숨겨진 의존성
**완화 전략**:
- Step 2에서 와일드카드 import를 먼저 제거
- 명시적 의존성 확인 후 이동

### 리스크 2: 숨겨진 DTO 참조
**완화 전략**:
- 각 단계마다 컴파일 검증
- 실패 시 즉시 롤백

### 리스크 3: 테스트 코드 영향
**완화 전략**:
- 각 도메인 이동 후 테스트 실행
- 실패 시 원인 분석 후 수정

### 리스크 4: Git 충돌
**완화 전략**:
- 도메인별 브랜치 생성
- 작은 단위로 PR 분리

---

## 💡 다음 단계 권장사항

**즉시 실행 가능**:
1. ✅ **Step 1: Lecture 중복 제거** (10분, 리스크 없음)
   - 가장 안전한 작업
   - Dead Code 제거로 혼란 방지

**후속 작업 순서**:
2. Step 2: 와일드카드 Import 제거 (20분)
3. Step 3-1: FCM 도메인 이동 (30분)
4. Step 3-2~3-5: 나머지 도메인 순차 이동

**총 예상 소요 시간**: 약 3~4시간 (검증 포함)

---

## 📌 참고 사항

### Spring Boot 컴포넌트 스캔
- Base package: `BlueCrab.com.example`
- DTO 패키지 이동해도 추가 설정 불필요
- `@ComponentScan` 영향 없음 (DTO는 `@Component` 아님)

### Git 커밋 전략
```bash
# 도메인별 브랜치 생성
git checkout -b refactor/dto-lecture-duplicate-removal
git checkout -b refactor/dto-wildcard-import-removal
git checkout -b refactor/dto-fcm-domain
git checkout -b refactor/dto-admin-domain
# ...
```

### IDE 지원
- IntelliJ IDEA: Refactor → Move → 자동 import 수정
- Eclipse: Refactor → Move → Update references

---

**분석 완료일**: 2025-10-15
**다음 작업**: Phase 2 - Step 1 (Lecture 중복 제거) 실행 대기
