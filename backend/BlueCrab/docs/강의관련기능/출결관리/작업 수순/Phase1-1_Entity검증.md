# Phase 1-1: DB 스키마 확인 및 Entity 검증

## 📋 작업 개요

**목표**: `ENROLLMENT_EXTENDED_TBL` 스키마 및 Entity 클래스 검증  
**소요 시간**: 1시간  
**상태**: ✅ 완료

---

## 🎯 작업 내용

### 1. DB 스키마 확인

#### 테이블 정보
- **테이블명**: `ENROLLMENT_EXTENDED_TBL`
- **주요 컬럼**:
  - `ENROLLMENT_IDX` (INT, PK, AUTO_INCREMENT)
  - `LEC_IDX` (INT, FK → LEC_TBL)
  - `STUDENT_IDX` (INT, FK → USER_TBL)
  - `ENROLLMENT_DATA` (LONGTEXT) - JSON 데이터

#### JSON 필드 구조
```json
{
  "enrollment": { ... },
  "attendance": {
    "summary": { ... },
    "sessions": [ ... ],
    "pendingRequests": [ ... ]
  },
  "gradeConfig": { ... },
  "grade": { ... }
}
```

### 2. Entity 클래스 검증

**파일 위치**: `src/main/java/BlueCrab/com/example/entity/Lecture/EnrollmentExtendedTbl.java`

#### 검증 항목
- [x] `@Entity` 및 `@Table` 어노테이션 확인
- [x] `enrollmentData` 필드가 `LONGTEXT`로 매핑되어 있는지 확인
- [x] `@Column(name = "ENROLLMENT_DATA", columnDefinition = "LONGTEXT")` 확인
- [x] Getter/Setter 메서드 존재 확인
- [x] `@ManyToOne` 관계 설정 확인 (LecTbl, UserTbl)

#### Entity 주요 코드
```java
@Entity
@Table(name = "ENROLLMENT_EXTENDED_TBL")
public class EnrollmentExtendedTbl {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENROLLMENT_IDX")
    private Integer enrollmentIdx;
    
    @Column(name = "LEC_IDX", nullable = false)
    private Integer lecIdx;
    
    @Column(name = "STUDENT_IDX", nullable = false)
    private Integer studentIdx;
    
    @Column(name = "ENROLLMENT_DATA", columnDefinition = "LONGTEXT")
    private String enrollmentData;
    
    // ... ManyToOne 관계 및 Getter/Setter
}
```

### 3. JSON 구조 명세서 작성

**파일 위치**: `docs/전체가이드/데이터구조/ENROLLMENT_DATA_JSON_명세서.md`

#### 작성 내용
- [x] `attendance.summary` 구조 정의
- [x] `attendance.sessions` 배열 구조 정의 (최대 80개)
- [x] `attendance.pendingRequests` 배열 구조 정의 (최대 80개)
- [x] 각 필드 설명 및 데이터 타입
- [x] 용량 분석 (24KB vs 4GB LONGTEXT 여유 확인)

### 4. 브라우저 콘솔 테스트 스크립트 작성

**파일 위치**: `docs/브라우저콘솔테스트/06-attendance/`

#### 생성된 파일
- [x] `README.md` - 테스트 가이드
- [x] `01-attendance-request.js` - 학생 출석 요청 테스트
- [x] `02-attendance-approve.js` - 교수 출석 승인 테스트

#### 주요 특징
- Prompt 기반 입력 (hardcoding 제거)
- 기본값 제공 (CS101, sessionNumber 1, studentIdx 6)
- JWT 토큰 자동 추출 (localStorage/sessionStorage)

---

## 📊 검증 결과

### Entity 구조 검증
✅ **통과**: `ENROLLMENT_DATA` 필드가 LONGTEXT로 올바르게 매핑됨

### 용량 분석
- **최악의 경우 예상 크기**: 약 24KB
- **LONGTEXT 최대 용량**: 4GB (4,294,967,295 bytes)
- **여유 공간**: 약 170,000배 여유 ✅

### JSON 구조 설계
✅ **검증 완료**:
- `summary`: 통계 정보 (6개 필드)
- `sessions`: 확정 출석 배열 (최대 80개)
- `pendingRequests`: 대기 요청 배열 (최대 80개)

---

## 🎯 다음 단계

**Phase 1-2: DTO 클래스 생성**
- DTO 인터페이스 설계
- Request/Response DTO 생성
- Validation 어노테이션 적용

---

## 📝 산출물

- ✅ Entity 클래스 검증 완료
- ✅ JSON 구조 명세서 작성
- ✅ 용량 분석 보고서
- ✅ 브라우저 콘솔 테스트 스크립트 (3개 파일)

---

## 📚 참고 문서

- [출석요청승인_플로우.md](../../출석요청승인_플로우.md)
- [ENROLLMENT_DATA_JSON_명세서.md](../../../전체가이드/데이터구조/ENROLLMENT_DATA_JSON_명세서.md)
- Entity 클래스: `EnrollmentExtendedTbl.java`
