# 게시판 API 문서

## 게시판 코드 (BOARD_CODE)

| 값 | 명칭     | 용도           |
|----|---------|---------------|
| 0  | 공지사항 | 학원 전체 공지  |
| 1  | 학사 정보 | 입학/학사일정  |
| 2  | 시설 정보 | 시설 안내/공지 |
| 3  | 강의 공지 | 강의별 공지    |

## 권한 매트릭스

| 게시판 코드 | 생성        | 조회       | 수정        | 삭제        |
|-----------|-----------|-----------|-----------|-----------|
| 0         | 관리자     | 전체       | 관리자     | 관리자     |
| 1         | 관리자     | 전체       | 관리자     | 관리자     |
| 2         | 관리자     | 전체       | 관리자     | 관리자     |
| 3         | 강사,관리자 | 수강생+작성자 | 작성자,관리자 | 작성자,관리자 |

## API 엔드포인트

### 1. 게시글 생성
```http
POST /api/boards/{boardCode}
Content-Type: multipart/form-data

{
  "title": "제목",
  "content": "내용",
  "lecSerial": "ETH201",  // 강의 공지(3)만 필수
  "files": [파일들]
}
```

**boardCode=3 (강의공지) 필수 필드**: `lecSerial`

### 2. 게시글 목록 조회

**전체 조회**
```http
GET /api/boards/list?boardCode=0
```

**특정 강의 공지 조회 (v1.1 수정)**
```http
GET /api/boards/list?boardCode=3&lecSerial=ETH201
```

**페이징**
```http
GET /api/boards/list?boardCode=0&page=0&size=10
```

**응답**
```json
{
  "data": [{
    "boardId": 1,
    "title": "제목",
    "content": "...",
    "lecSerial": "ETH201",
    "writeDate": "2024-01-01T10:00:00",
    "updateDate": "2024-01-01T10:00:00",
    "writer": "작성자명",
    "boardCode": 3,
    "fileExists": true
  }],
  "status": 200
}
```

### 3. 게시글 상세 조회
```http
GET /api/boards/{boardId}
```

## 테스트 명령어

**공지사항 생성**
```bash
curl -X POST http://localhost:8080/api/boards/0 ^
-H "Cookie: JSESSIONID=YOUR_SESSION" ^
-F "title=공지사항 테스트" ^
-F "content=테스트 내용"
```

**강의 공지 생성**
```bash
curl -X POST http://localhost:8080/api/boards/3 ^
-H "Cookie: JSESSIONID=YOUR_SESSION" ^
-F "title=강의공지" ^
-F "content=내용" ^
-F "lecSerial=ETH201"
```

**전체 공지 조회**
```bash
curl "http://localhost:8080/api/boards/list?boardCode=0"
```

**특정 강의 공지 조회**
```bash
curl "http://localhost:8080/api/boards/list?boardCode=3&lecSerial=ETH201"
```

**페이징 조회**
```bash
curl "http://localhost:8080/api/boards/list?boardCode=0&page=0&size=10"
```

## 오류 코드

| HTTP | 설명                    |
|------|------------------------|
| 400  | 필수 필드 누락/타입 오류  |
| 403  | 권한 없음              |
| 404  | 게시글 없음            |
| 500  | 서버 오류              |

## v1.1 버그 수정 (lecSerial 타입)

**문제**: `lecSerial`이 VARCHAR(10)인데 Controller가 Integer로 받아서 400 오류

**수정 파일**:
- `BoardController.java` (Line 61-98): `Map<String, Object>` 타입 변경
- `BoardService.java` (Line 61-68): lecSerial 조건 메서드 추가
- `BoardRepository.java` (Line 54-62): lecSerial JPQL 쿼리 추가

**수정 후**:
```http
GET /api/boards/list?boardCode=3&lecSerial=ETH201  ✅ 정상 작동
```

## 파일 구조

```
게시판/
├── README.md                    # 본 문서
├── backend-logic-flow.drawio    # 백엔드 플로우 다이어그램
├── test-1-create.js            # 생성 테스트
└── test-2-read.js              # 조회 테스트
```

## 다이어그램

`backend-logic-flow.drawio`: 생성 로직(좌) + 조회 로직(우) 통합 다이어그램
