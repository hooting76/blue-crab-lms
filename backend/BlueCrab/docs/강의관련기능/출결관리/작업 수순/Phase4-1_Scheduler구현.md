# Phase 4-1: 자동 승인 스케줄러 구현

## 📋 작업 개요

**목표**: 날짜 기준 7일 경과 시 자동 출석 승인 스케줄러 구현  
**예상 소요 시간**: 2시간  
**실제 소요 시간**: 2시간  
**상태**: ✅ 완료

---

## 🎯 작업 내용

### 1. 스케줄러 클래스 생성

**파일**: `src/main/java/BlueCrab/com/example/scheduler/AttendanceScheduler.java`

**구현 특징**:
- 날짜 기준 만료 처리 (시간 무관)
- 매일 오전 5시 실행
- 개별 실패 시에도 계속 진행
- 상세한 로깅

```java
@Component
@Slf4j
public class AttendanceScheduler {
    
    @Scheduled(cron = "0 0 5 * * *")  // 매일 오전 5시
    @Transactional
    public void processExpiredRequests() {
        LocalDate today = LocalDate.now();  // 날짜만 비교
        
        // 1. 모든 enrollment 조회
        List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findAll();
        
        for (EnrollmentExtendedTbl enrollment : enrollments) {
            // 2. JSON 파싱
            AttendanceDataDto attendanceData = parseAttendanceData(...);
            
            // 3. 만료된 요청 필터링 (날짜 기준)
            List<AttendancePendingRequestDto> expiredRequests = 
                attendanceData.getPendingRequests().stream()
                    .filter(request -> {
                        LocalDate expiresDate = request.getExpiresAt().toLocalDate();
                        return !expiresDate.isAfter(today);
                    })
                    .collect(Collectors.toList());
            
            // 4. sessions로 이동 (status: "출", tempApproved: true)
            for (AttendancePendingRequestDto expiredRequest : expiredRequests) {
                moveToSessions(attendanceData, expiredRequest);
            }
            
            // 5. pendingRequests에서 제거
            attendanceData.getPendingRequests().removeAll(expiredRequests);
            
            // 6. summary 재계산
            recalculateSummary(attendanceData);
            
            // 7. DB 저장
            enrollment.setEnrollmentData(updatedJson);
            enrollmentRepository.save(enrollment);
        }
    }
}
```

### 2. 스케줄링 활성화

**파일**: `BlueCrabApplication.java`

```java
@SpringBootApplication
@EnableScheduling  // 추가됨
public class BlueCrabApplication extends SpringBootServletInitializer {
    // ...
}
```

---

## 📋 체크리스트

- [x] `AttendanceScheduler` 클래스 생성
- [x] `@Scheduled` cron 설정 (매일 5시)
- [x] 날짜 기준 만료 요청 탐지 로직
- [x] 자동 승인 처리 로직 (moveToSessions)
- [x] summary 재계산 로직
- [x] JSON 파싱/직렬화 유틸리티
- [x] 예외 처리 (개별 실패 시 계속 진행)
- [x] 로깅 추가
- [x] `@EnableScheduling` 활성화
- [x] 수동 실행 메서드 (`manualTrigger`)
- [ ] summary 재계산
- [ ] 로깅 추가
- [ ] `@EnableScheduling` 활성화
- [ ] 배치 처리 최적화

---

## 🎯 다음 단계

**Phase 5-1: 단위 테스트**

---

## 📚 참고 문서

- [출석요청승인_플로우.md](../../출석요청승인_플로우.md)
- Service: `AttendanceService.java`
