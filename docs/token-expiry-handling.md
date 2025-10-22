# Expired JWT Handling (Frontend Guide)

## 1. 서버 응답 규약
- HTTP 상태 코드: `401 Unauthorized`
- 헤더
  - `X-Token-Expired: true`
  - `Access-Control-Allow-Origin: <요청 Origin>` (필터에서 보장)
- 본문(JSON)
  ```json
  {
    "success": false,
    "message": "토큰이 만료되었습니다. 리프레시 토큰을 사용하여 새 토큰을 발급받아주세요.",
    "data": null,
    "timestamp": "..."
  }
  ```

## 2. 프론트엔드 처리 절차
1. **만료 감지**  
   보호된 API 호출 결과가 401이며 `X-Token-Expired` 헤더가 true 또는 본문 메시지가 만료 안내라면 access token 만료로 판단한다.
2. **요청 중단**  
   현재 access token을 공용 상태(store/localStorage 등)에서 제거하거나 "만료" 상태로 표시하고 동일 토큰으로 추가 요청을 보내지 않는다.
3. **재발급 시도**  
   - refresh token이 유효하다면 토큰 재발급 API(예: `/api/auth/refresh`)로 새 access token을 요청한다.
   - refresh token도 만료되었거나 존재하지 않으면 즉시 로그인 화면으로 이동한다.
4. **원본 요청 재시도**  
   새 access token을 저장한 뒤 실패했던 API 요청을 동일 파라미터로 다시 실행한다.
5. **세션 종료**  
   재발급 요청이 401 또는 다른 오류로 실패하면 refresh token 및 사용자 세션을 전부 정리하고 로그인 페이지로 리다이렉트한다.

## 3. 구현 팁
- Axios 인터셉터 또는 fetch 래퍼에서 위 로직을 공통 처리하면 화면 단의 중복을 줄일 수 있다.
- 토큰 재발급 시 동시성 제어(예: 여러 요청에서 동시에 refresh를 호출하는 상황)를 고려해 큐잉 또는 상태 플래그를 사용한다.
- 재발급 성공 후에는 실패했던 요청에 최신 Authorization 헤더를 적용해 재호출한다.
- 사용자 경험을 위해 재로그인이 필요한 경우 토스트/다이얼로그로 안내하고, 필요한 경우 저장된 폼 데이터가 있으면 보존한다.

## 4. 체크리스트
- [ ] 401 응답에서 `X-Token-Expired` 확인 로직이 존재한다.
- [ ] 동일 access token으로 반복 요청하지 않는다.
- [ ] refresh 실패 시 세션 정리가 실행된다.
- [ ] 재발급 성공 후 원본 요청을 재시도한다.
- [ ] 사용자 알림 UX가 준비되어 있다.