# 🎨 Blue-Crab LMS 게시판 다이어그램 (신판)

> 최신 백엔드 구조와 API를 반영한 게시판 시스템 다이어그램 모음

## 📋 다이어그램 목록

### 1. 🏗️ [전체 아키텍처](./board-architecture.drawio)
- **파일**: `board-architecture.drawio`
- **설명**: Blue-Crab LMS 게시판 시스템의 전체 아키텍처 구조
- **포함 내용**:
  - 클라이언트 레이어 (React PWA, 브라우저 콘솔, Postman)
  - Spring Boot REST API Gateway
  - JWT Bearer Token 인증
  - Controller/Service/Repository/Entity 계층 구조
  - Board/Attachment 패키지 분리 현황
  - 데이터베이스 연결

### 2. 🔄 [API 플로우](./api-flow.drawio)
- **파일**: `api-flow.drawio`  
- **설명**: 게시판 및 첨부파일 API의 요청/응답 플로우
- **포함 내용**:
  - 게시글 CRUD API (생성/조회/상세)
  - 첨부파일 3단계 워크플로우
  - 파일 다운로드 및 정보 조회 API
  - 게시판 코드 (0-3) 정보
  - 인증 요구사항 및 파일 제한사항
  - API 응답 형식

### 3. 📎 [첨부파일 워크플로우 (상세)](./attachment-workflow-detailed.drawio)
- **파일**: `attachment-workflow-detailed.drawio`
- **설명**: 첨부파일 처리의 3단계 워크플로우 상세 분석
- **포함 내용**:
  - **1단계**: 게시글 생성 (boardIdx 반환)
  - **2단계**: 파일 업로드 (FormData → attachmentIdx[] 반환)
  - **3단계**: 첨부파일 연결 (boardIdx ↔ attachmentIdx 매핑)
  - 각 단계별 Controller → Service → Repository → Database 흐름
  - 데이터베이스 상태 변화
  - 보안 고려사항

### 4. 📁 [패키지 구조](./package-structure.drawio)
- **파일**: `package-structure.drawio`
- **설명**: Board/Attachment 하위 폴더로 재구성된 패키지 구조
- **포함 내용**:
  - `BlueCrab.com.example` 루트 패키지
  - `controller.Board` 및 `controller.Board.Attachment` 구조
  - `service.Board` 및 `service.Board.Attachment` 구조
  - `repository.Board` 및 `repository.Board.Attachment` 구조
  - `entity.Board` 및 `entity.Board.Attachment` 구조
  - `dto.Board.Attachment` 구조
  - 패키지 구조화 이점 및 마이그레이션 정보

## 🎯 사용 방법

### 1. Draw.io에서 열기
1. [https://app.diagrams.net/](https://app.diagrams.net/) 접속
2. "Open Existing Diagram" 클릭
3. 원하는 `.drawio` 파일 선택하여 업로드

### 2. 다크모드 지원
- 모든 다이어그램은 **흰색 배경**으로 설계
- 다크모드에서도 깔끔하게 표시됨
- 색상 구분이 명확하여 가독성 우수

### 3. 편집 및 수정
- Draw.io에서 직접 편집 가능
- 구조 변경 시 해당 다이어그램만 업데이트
- XML 형식으로 버전 관리 가능

## 🔄 업데이트 이력

### v2.0 (2025-10-27)
- ✅ 전체 아키텍처 다이어그램 생성
- ✅ API 플로우 다이어그램 생성  
- ✅ 첨부파일 워크플로우 상세 다이어그램 생성
- ✅ 패키지 구조 다이어그램 생성
- ✅ Board/Attachment 분리 구조 반영
- ✅ 최신 API 엔드포인트 반영
- ✅ 다크모드 호환성 확보

## 📋 파일 구성

```
다이어그램-신판/
├── README.md                           # 본 파일
├── board-architecture.drawio           # 전체 아키텍처
├── api-flow.drawio                     # API 플로우
├── attachment-workflow-detailed.drawio # 첨부파일 워크플로우
└── package-structure.drawio            # 패키지 구조
```

## 🎨 색상 가이드

| 구분 | 색상 | 용도 |
|------|------|------|
| 🟢 녹색 | `#4caf50` | 게시글 관련 컴포넌트 |
| 🔴 빨간색 | `#e57373` | 첨부파일 관련 컴포넌트 |
| 🔵 파란색 | `#1976d2` | 클라이언트 및 엔티티 |
| 🟠 주황색 | `#f57c00` | API 및 서비스 레이어 |
| 🟣 보라색 | `#9c27b0` | 리포지토리 레이어 |
| 🟡 노란색 | `#ffa000` | 설정 및 주요 정보 |

## 🔗 관련 문서

- [게시판 가이드문서](../가이드문서/README.md)
- [첨부파일 가이드](../첨부파일/README.md)
- [테스트 코드](../테스트코드/)
- [백엔드 소스 코드](../../src/main/java/BlueCrab/com/example/)

---

> **참고**: 이 다이어그램들은 2025년 10월 27일 기준 최신 백엔드 구조를 반영합니다. 
> 시스템 변경 시 해당 다이어그램도 함께 업데이트해 주세요.