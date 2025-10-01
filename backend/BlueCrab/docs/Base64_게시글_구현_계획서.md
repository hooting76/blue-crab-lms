# Base64 인코딩 게시글 시스템 구현 계획서

## 📋 프로젝트 개요

**목적**: 프론트엔드에서 게시글 제목과 본문을 Base64로 인코딩하여 백엔드로 전송하고, 데이터베이스에 Base64 형태로 저장하는 시스템 구현

**작성자**: 성태준  
**작성일**: 2025년 10월 1일  
**프로젝트명**: Blue Crab LMS - 게시글 Base64 인코딩 시스템

---

## 🎯 구현 목표

### 주요 기능
1. **프론트엔드**: 제목/본문을 Base64로 인코딩하여 백엔드 전송
2. **백엔드**: Base64 데이터 검증 및 DB 저장
3. **조회**: DB에서 Base64 데이터를 그대로 반환 (프론트엔드에서 디코딩)
4. **보안**: 원문 길이 제한을 통한 데이터 무결성 보장

### 데이터 흐름
```
[프론트엔드]                    [백엔드]                     [데이터베이스]
원문 작성                   Base64 수신                  Base64 저장
    ↓                          ↓                           ↓
Base64 인코딩              원문 길이 검증               TEXT 타입으로 저장
    ↓                          ↓                           ↓
API 전송                   DB 저장 (Base64)            게시글 데이터
    
[조회 시]
DB에서 Base64 조회 → 프론트엔드 전송 → 프론트엔드에서 디코딩
```

---

## 🛠️ 구현 단계별 계획

### 1단계: 데이터베이스 스키마 수정 📊
**목표**: BOARD_TBL 테이블의 제목/본문 컬럼을 Base64 저장에 적합하게 수정

#### 작업 내용
```sql
-- 현재 상태
BOARD_TIT VARCHAR(100)    → TEXT
BOARD_CONT VARCHAR(200)   → TEXT

-- 수정 쿼리
ALTER TABLE BOARD_TBL 
MODIFY COLUMN BOARD_TIT TEXT COMMENT '게시글 제목 (base64 인코딩)';

ALTER TABLE BOARD_TBL 
MODIFY COLUMN BOARD_CONT TEXT COMMENT '게시글 본문 (base64 인코딩)';
```

#### 변경 이유
- **VARCHAR 길이 제한 해결**: Base64 인코딩 시 데이터 크기가 약 33% 증가
- **확장성**: 향후 더 긴 게시글 지원 가능
- **유연성**: TEXT 타입으로 최대 65,535 바이트까지 저장 가능

---

### 2단계: JPA 엔티티 수정 🏗️
**목표**: BoardTbl.java 엔티티를 TEXT 타입에 맞게 수정

#### 작업 내용
**파일**: `src/main/java/BlueCrab/com/example/entity/BoardTbl.java`

```java
// 기존 코드
@Size(max = 100, message = "게시글 제목은 100자를 초과할 수 없습니다")
@Column(name = "BOARD_TIT", length = 100, columnDefinition = "VARCHAR", nullable = true)
private String boardTitle = "공지사항";

@Size(max = 200, message = "게시글 내용은 200자를 초과할 수 없습니다")
@Column(name = "BOARD_CONT", length = 200, columnDefinition = "VARCHAR", nullable = true)
private String boardContent;

// 수정 후
@NotBlank(message = "게시글 제목은 필수입니다")
@Lob
@Column(name = "BOARD_TIT", columnDefinition = "TEXT", nullable = true)
private String boardTitle = "공지사항";

@NotBlank(message = "게시글 내용은 필수입니다")
@Lob
@Column(name = "BOARD_CONT", columnDefinition = "TEXT", nullable = true)
private String boardContent;
```

#### 필요한 Import 추가
```java
import javax.persistence.Lob;
```

#### 제거할 Import
```java
import javax.validation.constraints.Size; // 더 이상 사용하지 않음
```

---

### 3단계: Base64 검증 유틸리티 클래스 생성 🔧
**목표**: Base64 인코딩된 데이터의 원문 길이 검증 및 디코딩 기능 제공

#### 작업 내용
**파일**: `src/main/java/BlueCrab/com/example/util/Base64ValidationUtil.java`

```java
public class Base64ValidationUtil {
    // 원문 길이 제한
    public static final int MAX_TITLE_LENGTH = 500;        // 제목 원문 최대 500자
    public static final int MAX_CONTENT_LENGTH = 10000;    // 본문 원문 최대 10,000자
    
    // 주요 메서드
    public static boolean isValidEncodedTitleLength(String encodedTitle)
    public static boolean isValidEncodedContentLength(String encodedContent)
    public static String decodeBase64(String encodedString)
    public static String getTitleLengthErrorMessage(int actualLength)
    public static String getContentLengthErrorMessage(int actualLength)
}
```

#### 기능
- Base64 디코딩 및 원문 길이 검증
- 에러 메시지 생성
- 유효성 검사 결과 반환

---

### 4단계: 게시글 작성 컨트롤러 수정 📝
**목표**: BoardCreateController에 Base64 검증 로직 추가

#### 작업 내용
**파일**: `src/main/java/BlueCrab/com/example/controller/BoardCreateController.java`

```java
// JWT 검증 후 Base64 검증 로직 추가
// ========== Base64 인코딩된 제목과 본문의 원문 길이 검증 ==========
try {
    // 제목 길이 검증
    if (!Base64ValidationUtil.isValidEncodedTitleLength(boardTbl.getBoardTitle())) {
        // 에러 응답 반환
    }
    
    // 본문 길이 검증
    if (!Base64ValidationUtil.isValidEncodedContentLength(boardTbl.getBoardContent())) {
        // 에러 응답 반환
    }
    
} catch (IllegalArgumentException e) {
    // Base64 디코딩 실패 시 에러 응답
}
```

#### 추가 Import
```java
import BlueCrab.com.example.util.Base64ValidationUtil;
```

---

### 5단계: 게시글 수정 컨트롤러 수정 ✏️
**목표**: BoardUpdateController에 Base64 검증 로직 추가

#### 작업 내용
**파일**: `src/main/java/BlueCrab/com/example/controller/BoardUpdateController.java`

- BoardCreateController와 동일한 검증 로직 적용
- JWT 검증 후 Base64 검증 수행
- 동일한 에러 응답 패턴 사용

---

## 📋 에러 응답 설계

### 1. 제목 길이 초과
```json
{
    "success": false,
    "message": "제목은 500자를 초과할 수 없습니다. (현재: 600자)",
    "errorCode": "TITLE_LENGTH_EXCEEDED"
}
```

### 2. 본문 길이 초과
```json
{
    "success": false,
    "message": "본문은 10000자를 초과할 수 없습니다. (현재: 12000자)",
    "errorCode": "CONTENT_LENGTH_EXCEEDED"
}
```

### 3. Base64 디코딩 실패
```json
{
    "success": false,
    "message": "제목 또는 본문의 Base64 인코딩이 올바르지 않습니다.",
    "errorCode": "INVALID_BASE64_ENCODING"
}
```

---

## 🔍 테스트 시나리오

### 1. 정상 시나리오
1. 프론트엔드에서 제목/본문을 Base64로 인코딩
2. 백엔드에서 Base64 검증 성공
3. 원문 길이가 제한 내에 있음
4. DB에 Base64 형태로 저장 성공

### 2. 길이 초과 시나리오
1. 제목이 500자를 초과하는 원문
2. Base64 인코딩 후 백엔드 전송
3. 백엔드에서 디코딩 후 길이 검증 실패
4. 적절한 에러 메시지와 함께 400 응답

### 3. 잘못된 Base64 시나리오
1. 잘못된 Base64 문자열 전송
2. 백엔드에서 디코딩 시도 시 예외 발생
3. Base64 디코딩 실패 에러 응답

---

## 📁 파일 구조

```
src/main/java/BlueCrab/com/example/
├── entity/
│   └── BoardTbl.java                    # [수정] @Lob 추가, length 제한 제거
├── controller/
│   ├── BoardCreateController.java       # [수정] Base64 검증 로직 추가
│   └── BoardUpdateController.java       # [수정] Base64 검증 로직 추가
└── util/
    └── Base64ValidationUtil.java        # [신규] Base64 검증 유틸리티
```

---

## ⚠️ 주의사항

### 1. 보안 고려사항
- 원문 길이 제한을 통한 DoS 공격 방지
- Base64 디코딩 실패 시 적절한 에러 처리
- JWT 토큰 검증 후 Base64 검증 수행

### 2. 성능 고려사항
- Base64 디코딩은 검증 시에만 수행
- DB 저장은 Base64 형태로 유지 (원문 저장 안 함)
- 조회 시 디코딩은 프론트엔드에서 처리

### 3. 호환성
- 기존 게시글 데이터와의 호환성 고려 필요
- 마이그레이션 계획 수립 (필요 시)

---

## 📈 구현 순서 및 일정

| 단계 | 작업 내용 | 예상 시간 | 의존성 |
|------|-----------|----------|---------|
| 1 | DB 스키마 수정 | 10분 | 없음 |
| 2 | BoardTbl 엔티티 수정 | 15분 | 1단계 완료 |
| 3 | Base64ValidationUtil 생성 | 30분 | 없음 |
| 4 | BoardCreateController 수정 | 20분 | 2, 3단계 완료 |
| 5 | BoardUpdateController 수정 | 20분 | 2, 3단계 완료 |
| 6 | 테스트 및 검증 | 30분 | 전체 완료 |

**총 예상 시간**: 약 2시간

---

## ✅ 완료 체크리스트

- [ ] BOARD_TBL 스키마 수정 (TEXT 타입 변경)
- [ ] BoardTbl.java 엔티티 수정 (@Lob 추가)
- [ ] Base64ValidationUtil.java 생성
- [ ] BoardCreateController.java 수정
- [ ] BoardUpdateController.java 수정
- [ ] 에러 응답 테스트
- [ ] 정상 동작 테스트
- [ ] 문서화 완료

---

## 📞 연락처 및 지원

**개발자**: 성태준  
**프로젝트**: Blue Crab LMS  
**문서 버전**: 1.0  
**최종 수정일**: 2025년 10월 1일