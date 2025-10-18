# BlueCrab 백엔드 API 엔드포인트 완전 분석

> **분석 일자**: 2025-09-28  
> **분석 범위**: 전체 Controller API 엔드포인트  
> **총 Controller 수**: 14개  
> **총 API 엔드포인트 수**: 48개

## 📚 API 카테고리별 완전 정리

### 🔐 **1. 인증/보안 관련 API (16개 엔드포인트)**

#### **AuthController** `/api/auth` - 기본 사용자 인증 (4개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| POST | `/api/auth/login` | 사용자 로그인 | ❌ | ✅ 완료 |
| POST | `/api/auth/refresh` | JWT 토큰 갱신 | ❌ | ✅ 완료 |
| GET | `/api/auth/validate` | 토큰 유효성 검증 | ✅ | ✅ 완료 |
| POST | `/api/auth/logout` | 사용자 로그아웃 | ✅ | ✅ 완료 |

#### **AdminController** `/api/admin` - 관리자 인증 (2개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| POST | `/api/admin/login` | 관리자 1차 로그인 | ❌ | ✅ 완료 |
| GET | `/api/admin/verify-email` | 이메일 인증 확인 | ❌ | ✅ 완료 |

#### **AdminEmailAuthController** `/api/admin/email-auth` - 관리자 이메일 인증 (2개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| POST | `/api/admin/email-auth/request` | 관리자 이메일 인증 요청 | 🟡 세션토큰 | ✅ 완료 |
| POST | `/api/admin/email-auth/verify` | 관리자 이메일 인증 검증 | 🟡 세션토큰 | ✅ 완료 |

#### **PasswordResetController** `/api/auth/password-reset` - 비밀번호 재설정 (5개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| POST | `/api/auth/password-reset/verify-identity` | 본인 확인 | ❌ | ✅ 완료 |
| GET | `/api/auth/password-reset/rate-limit-status` | 속도 제한 상태 확인 | ❌ | ✅ 완료 |
| POST | `/api/auth/password-reset/send-email` | 인증 이메일 발송 | ❌ | ✅ 완료 |
| POST | `/api/auth/password-reset/verify-code` | 인증 코드 검증 | ❌ | ✅ 완료 |
| POST | `/api/auth/password-reset/change-password` | 비밀번호 변경 | ❌ | ✅ 완료 |

#### **FindIdController** `/api/account` - 아이디 찾기 (1개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| POST | `/api/account/FindId` | 아이디 찾기 | ❌ | ✅ 완료 |

#### **MailAuthCheckController** - 메일 인증 (2개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| GET | `/sendMail` | 이메일 인증 코드 발송 | ❌ | ⚠️ 경로 이상 |
| POST | `/verifyCode` | 인증 코드 확인 | ❌ | ⚠️ 경로 이상 |

### 👥 **2. 사용자/프로필 관리 API (12개 엔드포인트)**

#### **UserController** `/api/users` - 사용자 관리 (9개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| GET | `/api/users` | 전체 사용자 조회 | ✅ | ✅ 완료 |
| GET | `/api/users/students` | 학생 사용자 조회 | ✅ | ✅ 완료 |
| GET | `/api/users/professors` | 교수 사용자 조회 | ✅ | ✅ 완료 |
| GET | `/api/users/{id}` | 특정 사용자 조회 | ✅ | ✅ 완료 |
| POST | `/api/users` | 사용자 생성 | ✅ | ✅ 완료 |
| PUT | `/api/users/{id}` | 사용자 정보 수정 | ✅ | ✅ 완료 |
| DELETE | `/api/users/{id}` | 사용자 삭제 | ✅ | ✅ 완료 |
| GET | `/api/users/search` | 사용자 검색 | ✅ | ✅ 완료 |
| GET | `/api/users/search-all` | 전체 검색 | ✅ | ✅ 완료 |
| GET | `/api/users/search-birth` | 생년월일 검색 | ✅ | ✅ 완료 |
| GET | `/api/users/stats` | 사용자 통계 | ✅ | ✅ 완료 |

#### **ProfileController** `/api/profile` - 프로필 관리 (3개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| POST | `/api/profile/me` | 내 프로필 조회 | ✅ | ✅ 완료 |
| POST | `/api/profile/me/completeness` | 프로필 완성도 체크 | ✅ | ✅ 완료 |
| POST | `/api/profile/me/image` | 프로필 이미지 URL 조회 | ✅ | ✅ 완료 |
| POST | `/api/profile/me/image/file` | 프로필 이미지 파일 조회 | ✅ | ✅ 완료 |

### 📝 **3. 콘텐츠 관리 API (8개 엔드포인트)**

#### **BoardController** `/api/boards` - 게시판 관리 (8개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| POST | `/api/boards/create` | 게시글 작성 | ✅ | ✅ 완료 |
| POST | `/api/boards/list` | 게시글 목록 조회 (페이징) | ✅ | ✅ 완료 |
| POST | `/api/boards/detail` | 게시글 상세 조회 | ✅ | ✅ 완료 |
| PUT | `/api/boards/update/{boardIdx}` | 게시글 수정 | ✅ | ✅ 완료 |
| DELETE | `/api/boards/delete/{boardIdx}` | 게시글 삭제 | ✅ | ✅ 완료 |
| POST | `/api/boards/count` | 게시글 총 개수 조회 | ✅ | ✅ 완료 |
| POST | `/api/boards/exists` | 게시글 존재 여부 확인 | ✅ | ✅ 완료 |
| POST | `/api/boards/bycode` | 코드별 게시글 조회 | ✅ | ✅ 완료 |
| POST | `/api/boards/count/bycode` | 코드별 게시글 개수 | ✅ | ✅ 완료 |

### 🛠️ **4. 시스템/관리 API (12개 엔드포인트)**

#### **ApiController** `/api` - 시스템 상태 체크 (5개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| GET | `/api/health` | 시스템 헬스체크 | ❌ | ✅ 완료 |
| GET | `/api/test` | API 연결 테스트 | ❌ | ✅ 완료 |
| GET | `/api/system-info` | 시스템 정보 조회 | ❌ | ✅ 완료 |
| GET | `/api/test-auth` | 인증 테스트 | ✅ | ✅ 완료 |
| GET | `/api/ping` | 간단한 ping 테스트 | ❌ | ✅ 완료 |

#### **DatabaseController** `/api/database` - 데이터베이스 관리 (5개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| GET | `/api/database/info` | 데이터베이스 정보 조회 | ✅ | ✅ 완료 |
| GET | `/api/database/tables` | 테이블 목록 조회 | ✅ | ✅ 완료 |
| GET | `/api/database/tables/{tableName}/columns` | 테이블 컬럼 정보 | ✅ | ✅ 완료 |
| GET | `/api/database/tables/{tableName}/sample` | 테이블 샘플 데이터 | ✅ | ✅ 완료 |
| GET | `/api/database/query` | SQL 쿼리 실행 | ✅ | ✅ 완료 |

#### **MetricsController** `/admin/metrics` - 시스템 메트릭 (2개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| GET | `/admin/metrics/system` | 시스템 메트릭 조회 | ✅ | ✅ 완료 |
| GET | `/admin/metrics/logs` | 로그 메트릭 조회 | ✅ | ✅ 완료 |

#### **LogMonitorController** `/admin/logs` - 로그 모니터링 (3개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| GET | `/admin/logs/monitor` | 로그 모니터링 페이지 | ✅ | ✅ 완료 |
| GET | `/admin/logs/stream` | 실시간 로그 스트림 | ✅ | ✅ 완료 |
| GET | `/admin/logs/status` | 로그 상태 조회 | ✅ | ✅ 완료 |

#### **HomeController** - 홈페이지 (1개)
| Method | Endpoint | 기능 | 인증 필요 | 상태 |
|--------|----------|------|-----------|------|
| GET | `/` | 홈페이지 | ❌ | ✅ 완료 |

## 🚨 **API 설계 이슈 분석**

### ⚠️ **심각한 문제 (Critical Issues)**

#### 1. **일관성 없는 HTTP 메서드 사용**
```
❌ 문제 사례:
- BoardController: 조회 기능에 POST 사용
  - POST /api/boards/list (조회) → GET으로 변경 필요
  - POST /api/boards/detail (조회) → GET으로 변경 필요
  - POST /api/boards/count (조회) → GET으로 변경 필요

- ProfileController: 조회 기능에 POST 사용
  - POST /api/profile/me (조회) → GET으로 변경 필요
  - POST /api/profile/me/completeness (조회) → GET으로 변경 필요
```

#### 2. **URL 경로 일관성 문제**
```
❌ 문제 사례:
- MailAuthCheckController: 기본 경로 없음
  - /sendMail → /api/mail/auth/send 
  - /verifyCode → /api/mail/auth/verify

- FindIdController: 비일관적 네이밍
  - /api/account/FindId → /api/account/find-id (kebab-case)

- MetricsController vs 다른 Controller
  - /admin/metrics → /api/admin/metrics (일관성)
```

#### 3. **중복 기능 의심**
```
⚠️ 중복 가능성:
- 관리자 인증: AdminController + AdminEmailAuthController
- 이메일 인증: PasswordResetController + MailAuthCheckController  
- 사용자 검색: UserController에 3개의 검색 엔드포인트
```

### 🟡 **중간 우선순위 문제 (Medium Issues)**

#### 1. **응답 형식 일관성**
- 대부분 `ApiResponse<T>` 사용하지만 일부 예외 존재
- 에러 응답 형식 표준화 필요

#### 2. **인증 방식 복잡성**
- 일반 사용자: JWT Bearer Token
- 관리자: 세션 토큰 → JWT 토큰 (2단계)
- 비인증: 공개 API

#### 3. **페이징 처리 불일치**
- BoardController: POST 방식 페이징
- UserController: GET 방식 페이징 (쿼리 파라미터)

## 💡 **리팩토링 제안사항**

### 🎯 **1단계: HTTP 메서드 표준화 (높은 우선순위)**
```java
// 변경 전 (잘못된 방식)
@PostMapping("/list")
public ResponseEntity<?> getAllBoards(@RequestBody Map<String, Integer> request)

// 변경 후 (올바른 방식)  
@GetMapping("/list")
public ResponseEntity<?> getAllBoards(@RequestParam int page, @RequestParam int size)
```

### 🎯 **2단계: URL 경로 표준화**
```
현재: /api/account/FindId
제안: /api/account/find-id

현재: /sendMail, /verifyCode  
제안: /api/mail/auth/send, /api/mail/auth/verify

현재: /admin/metrics, /admin/logs
제안: /api/admin/metrics, /api/admin/logs
```

### 🎯 **3단계: Controller 통합 검토**
- **AdminController + AdminEmailAuthController**: 통합 가능성 검토
- **PasswordResetController vs MailAuthCheckController**: 기능 중복 제거
- **UserController**: 검색 기능 별도 Controller 분리 고려

### 🎯 **4단계: 공통 기능 추상화**
- 페이징 처리 표준화
- 응답 형식 완전 통일
- 에러 처리 표준화

## 📊 **API 사용 빈도 예상 분석**

### 🔥 **높은 사용 빈도 (High Usage)**
- `/api/auth/login`, `/api/auth/logout` - 필수 인증
- `/api/profile/me` - 사용자 정보 조회
- `/api/boards/list`, `/api/boards/detail` - 게시판 조회
- `/api/health`, `/api/ping` - 시스템 모니터링

### 🟡 **중간 사용 빈도 (Medium Usage)**  
- `/api/users/*` - 관리자 기능
- `/api/boards/create`, `/api/boards/update` - 게시글 작성/수정
- `/api/auth/password-reset/*` - 비밀번호 재설정

### 🟢 **낮은 사용 빈도 (Low Usage)**
- `/api/database/*` - 개발/관리용
- `/admin/logs/*`, `/admin/metrics/*` - 모니터링
- `/api/system-info` - 시스템 정보

---

### 📋 **다음 단계 (Phase 2)**
1. **각 Controller별 상세 코드 분석**
2. **Service 계층 의존성 매핑**  
3. **보안 취약점 분석**
4. **성능 최적화 포인트 식별**

*이 문서는 API 엔드포인트 완전 분석 결과이며, 리팩토링 우선순위에 따라 단계적 개선을 진행할 예정입니다.*