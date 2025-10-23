# Phase 3-1: Controller 구현

## 📋 작업 개요

**목표**: 출석 요청/승인 REST API 엔드포인트 구현  
**예상 소요 시간**: 2시간  
**상태**: ✅ 완료 (2025-10-23)

---

## 🎯 주요 API 엔드포인트

### 1. 출석 요청 API (학생용)
- **URL**: `POST /api/attendance/request`
- **권한**: 학생 (authenticated)
- **Request Body**: `AttendanceRequestRequestDto`
- **Response**: `AttendanceResponseDto<AttendanceDataDto>`

### 2. 출석 승인 API (교수용)
- **URL**: `POST /api/attendance/approve`
- **권한**: 교수/관리자
- **Request Body**: `AttendanceApproveRequestDto`
- **Response**: `AttendanceResponseDto<Void>`

### 3. 학생 출석 조회 API
- **URL**: `POST /api/attendance/student/view`
- **권한**: 학생 (본인만)
- **Request Body**: `{ "lecSerial": "..." }`
- **Response**: `AttendanceResponseDto<AttendanceDataDto>`

### 4. 교수 출석 조회 API
- **URL**: `POST /api/attendance/professor/view`
- **권한**: 교수/관리자
- **Request Body**: `{ "lecSerial": "..." }`
- **Response**: `AttendanceResponseDto<List<StudentAttendanceDto>>`

---

## 📋 체크리스트

- [x] `AttendanceController` 클래스 생성
- [x] `@RestController`, `@RequestMapping` 설정
- [x] POST /api/attendance/request 구현
- [x] POST /api/attendance/approve 구현
- [x] POST /api/attendance/student/view 구현
- [x] POST /api/attendance/professor/view 구현
- [x] `@Valid` Validation 적용
- [x] JWT 토큰에서 USER_IDX 추출
- [x] 로깅 추가
- [x] 예외 핸들링

---

## 🎯 다음 단계

**Phase 3-2: Security 설정**
- Spring Security 권한 설정
- CORS 설정

---

## 📚 참고 문서

- Service: `AttendanceService.java`
- DTO: `dto/Lecture/attendance/`
