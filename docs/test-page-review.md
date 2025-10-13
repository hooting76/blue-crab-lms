# 테스트 페이지 점검 보고서

## 개요
BlueCrab LMS의 브라우저 테스트 페이지(`backend/BlueCrab/api-test.html`, `backend/BlueCrab/api-test-dual.html`, `backend/BlueCrab/attachment-test.html`)를 검토해 발견한 문제점을 정리했습니다. 각 항목마다 현재 동작, 원인, 권장 조치를 포함합니다.

## 핵심 이슈

1. **JWT 로그인 결과 파싱 오류**  
   - 위치: `backend/BlueCrab/api-test.html:321`, `backend/BlueCrab/api-test-dual.html:633`  
   - 현상: 로그인 성공 후 `data.data.userInfo`를 `localStorage`에 저장하고 다시 `JSON.parse`하는 과정에서 `JSON.parse(undefined)`가 발생 → SyntaxError로 이어져 UI에 "네트워크 오류" 메시지 표출.  
   - 원인: 백엔드 `LoginResponse`가 사용자 정보를 `data.user`에 담아 반환함 (`backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/LoginResponse.java:8`).  
   - 조치: 응답 구조에 맞춰 `data.data.user`를 저장하고, 토큰 정보 표시 시에도 동일 필드를 사용하거나 파싱 실패를 안전하게 처리.

2. **권한 헤더 누락으로 다수 엔드포인트 401 발생**  
   - 위치: `backend/BlueCrab/api-test.html:407`, `backend/BlueCrab/api-test-dual.html:722`  
   - 현상: `test-auth`, `users`, `admin` 같은 특정 문자열이 URL에 포함될 때만 Authorization 헤더를 붙임.  
   - 원인: Spring Security 설정에서 `/api/auth/**`, `/api/ping`, `/api/boards/**` 등을 제외한 `/api/**` 요청은 인증 필요 (`backend/BlueCrab/src/main/java/BlueCrab/com/example/config/SecurityConfig.java:120`).  
   - 영향: `health`, `system-info`, `test`, `notices`, `profile`, `courses` 등 대부분의 엔드포인트가 로그인 후에도 401.  
   - 조치: 기본적으로 `/api/`로 시작하는 요청에는 모두 JWT를 첨부하고, permitAll 엔드포인트만 예외 처리하도록 조건을 재구성.

3. **첨부파일 업로드 API 경로·파라미터 불일치**  
   - 위치: `backend/BlueCrab/attachment-test.html:702`  
   - 현상: 업로드 요청을 `POST /api/attachments/upload`로 보내고 FormData 키를 `file`로 사용.  
   - 원인: 실제 백엔드 업로드 엔드포인트는 `POST /api/board-attachments/upload/{boardIdx}`이며 파일 파라미터 이름은 `files` (`backend/BlueCrab/src/main/java/BlueCrab/com/example/controller/BoardAttachmentUploadController.java:51`).  
   - 결과: 잘못된 URL/파라미터로 인해 404 또는 “Required request parameter 'files'” 오류.  
   - 조치: 게시글 식별자를 경로에 포함하고 FormData 키를 `files`로 맞춘 뒤 업로드 전에 게시글을 생성하거나 임시 ID를 사용할 수 있도록 UI 흐름 수정.

4. **첨부파일 연결 요청 필드명 불일치**  
   - 위치: `backend/BlueCrab/attachment-test.html:739`  
   - 현상: 게시글-첨부파일 연결 시 본문을 `{ attachmentIds: [...] }`로 전송.  
   - 원인: 서버 DTO `AttachmentLinkRequest`는 `attachmentIdx` 리스트를 기대함 (`backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/AttachmentLinkRequest.java:17`).  
   - 결과: 유효성 검사에서 “첨부파일 IDX 목록은 필수입니다” 에러.  
   - 조치: 요청 본문 필드를 `attachmentIdx`로 수정하거나 백엔드 DTO를 스펙에 맞게 조정.

## 추가 권장 사항
- PermitAll 엔드포인트 목록을 재확인해 테스트 페이지가 자동으로 구분할 수 있도록 상수화하면 유지보수가 쉬워집니다.
- 로컬 저장소에 저장한 토큰/사용자 정보가 없을 때를 대비해 `updateTokenInfo`에서 방어 코드를 추가하면 예외 노출을 줄일 수 있습니다.
- 첨부파일 테스트 페이지는 게시글 생성 → 파일 업로드 → 첨부 연결 흐름을 명확히 안내하거나, 서버 API 사양에 맞춘 순서대로 자동 실행하도록 개선하는 것이 좋습니다.
