# Phase 5: 테스트 및 검증

## 📋 작업 개요

**목표**: 구현된 출석 시스템의 단위/통합/API 테스트  
**예상 소요 시간**: 7시간 (단위 3h + 통합 2h + API 2h)  
**상태**: 🟡 대기

---

## 🎯 테스트 범위

### Phase 5-1: 단위 테스트 (3시간)
- [ ] Repository 메서드 테스트
- [ ] Service 로직 테스트
- [ ] JSON 파싱/직렬화 테스트
- [ ] 출석률 계산 로직 테스트
- [ ] 중복 검증 로직 테스트

### Phase 5-2: 통합 테스트 (2시간)
- [ ] Controller 통합 테스트
- [ ] Service-Repository 통합 테스트
- [ ] 트랜잭션 롤백 테스트
- [ ] 권한 검증 테스트

### Phase 5-3: API 테스트 (2시간) ✅ **완료 (100%)**
- [x] 브라우저 콘솔 테스트 스크립트 작성 (4개 파일)
- [x] 로그인 테스트 (JWT 토큰 발급)
- [x] 출석 요청 API 테스트 (학생) ✅ **신규 완료**
- [x] 출석 승인 API 테스트 (교수)
- [x] 학생 출석 조회 API 테스트 ✅ **신규 완료**
- [x] 교수 출석 조회 API 테스트
- [x] 엔드-투-엔드 시나리오 테스트 (요청→승인→조회) ✅ **신규 완료**
- [x] 토큰 우선순위 수정 (window.authToken 우선)
- [x] Repository 쿼리 최적화 (professor verification)
- [x] 응답 필드 매핑 수정 (attendanceData 구조)
- [ ] Postman/Insomnia 테스트 (선택사항)
- [ ] 스케줄러 수동 실행 테스트 (다음 단계)

---

## 📋 주요 테스트 시나리오

### 1. 출석 요청 시나리오 ✅ **완료**
1. ✅ 학생이 출석 요청 (ETH201 강의, sessionNumber)
2. ✅ pendingRequests에 추가 확인
3. ✅ requestDate, expiresAt 자동 설정 확인
4. ✅ tempApproved=true 상태 확인

### 2. 출석 승인 시나리오 ✅ **완료**
1. ✅ 교수가 출석 승인 (ETH201 강의, sessionNumber)
2. ✅ pendingRequests에서 제거 확인
3. ✅ sessions에 추가 확인 (status="출", approvedBy=25)
4. ✅ summary 재계산 확인 (attended, attendanceRate)

### 3. 출석 조회 시나리오 ✅ **완료**
1. ✅ 학생이 본인 출석 조회 (summary, sessions, pendingRequests)
2. ✅ 교수가 전체 학생 출석 조회 (attendanceData 중첩 구조)
3. ✅ 필드명 정확성 확인 (approvedDate, requestDate)
4. ✅ 통계 계산 정확성 확인 (attended, late, absent, attendanceRate)

### 3. 권한 검증 시나리오
1. 학생 → 타 학생 출석 조회 (실패)
2. 교수 → 비담당 강의 승인 (실패)
3. 정상 권한 확인 (성공)

---

## 🎯 다음 단계

**Phase 6-1: API 문서화**

---

## 📚 참고 문서

- [브라우저콘솔테스트/06-attendance/](../../../브라우저콘솔테스트/06-attendance/)
- 테스트 프레임워크: JUnit 5, Mockito
