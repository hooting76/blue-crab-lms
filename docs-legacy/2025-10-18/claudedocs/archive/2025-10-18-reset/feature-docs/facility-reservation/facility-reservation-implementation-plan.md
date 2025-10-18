# 시설물 예약 시스템 구현 계획서 (최종판)

> **기반**: 열람실(ReadingRoom) 모듈 패턴 + GPT-5 청사진 + 완전한 코드 구현
> **목표**: 프론트엔드 연동 가능한 완성형 백엔드 모듈 (사용자 + 관리자)

## 📋 목차
1. [개요](#1-개요)
2. [기능 요구사항](#2-기능-요구사항)
3. [데이터 모델](#3-데이터-모델)
4. [Repository 계층](#4-repository-계층)
5. [DTO 설계](#5-dto-설계)
6. [Service 로직](#6-service-로직)
7. [Controller 설계](#7-controller-설계)
8. [Config & 정책](#8-config--정책)
9. [스케줄러](#9-스케줄러)
10. [구현 순서](#10-구현-순서)

---

## 1. 개요

### 1.1 아키텍처
```
Controller → Service → Repository → Entity → DB
     ↓          ↓           ↓          ↓
   DTO ←─── Business ──→ JPA ───→ Table
```

### 1.2 핵심 특징
- **JWT 인증**: 모든 API는 Bearer 토큰 필수
- **관리자 승인 워크플로우**: PENDING → APPROVED/REJECTED
- **감사 로그**: 모든 상태 전이 추적
- **통계 대시보드**: 관리자 실시간 집계
- **Rate Limit**: 남용 방지
- **스케줄러**: 자동 만료 처리 + 통계 집계

### 1.3 프론트엔드 연동
- 사용자: `FacilityBookingSystem` 컴포넌트
- 관리자: `AdminBookingSystem` 컴포넌트
- 응답 형식: `ApiResponse<T>` 통일

---

## 2. 기능 요구사항

### 2.1 사용자 기능
| 기능 | API 엔드포인트 | 설명 |
|------|---------------|------|
| 시설물 목록 조회 | `POST /api/facilities/list` | 타입별 필터링, 검색 |
| 시설물 상세 조회 | `POST /api/facilities/{id}/detail` | 위치, 수용인원, 장비, 다가오는 예약 |
| 예약 가능 시간대 조회 | `POST /api/facilities/{id}/availability` | 일자별 1시간 슬롯 (09:00~21:00) |
| 예약 가능 여부 확인 | `POST /api/facility-reservations/check` | 중복 체크, 정책 검증 |
| 예약 생성 | `POST /api/facility-reservations/reserve` | 승인 대기 상태로 생성 |
| 내 예약 조회 | `POST /api/facility-reservations/my` | 진행중/완료 탭 분리 |
| 예약 취소 | `POST /api/facility-reservations/cancel` | 본인 예약, 시작 2시간 전까지 |

### 2.2 관리자 기능
| 기능 | API 엔드포인트 | 설명 |
|------|---------------|------|
| 승인 대기 목록 | `POST /api/admin/facility-reservations/pending` | 필터링 지원 |
| 전체 예약 목록 | `POST /api/admin/facility-reservations/list` | 페이징, 정렬, 필터 |
| 대시보드 통계 | `POST /api/admin/facility-reservations/stats` | 오늘/주/월 집계 |
| 예약 승인 | `POST /api/admin/facility-reservations/{id}/approve` | 메모 저장 |
| 예약 반려 | `POST /api/admin/facility-reservations/{id}/reject` | 반려 사유 필수 |
| 시설물 관리 | `POST /api/admin/facilities/*` | CRUD (선택 사항) |

---

## 3. 데이터 모델

### 3.1 Entity: `Facility.java`
```java
package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설물 정보 엔티티
 * FACILITY_TBL 테이블과 매핑
 */
@Entity
@Table(name = "FACILITY_TBL")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACILITY_IDX")
    private Integer facilityId;

    @Column(name = "FACILITY_CODE", length = 50)
    private String facilityCode; // 검색용 코드 (선택)

    @Column(name = "FACILITY_NAME", nullable = false, length = 100)
    private String facilityName;

    @Column(name = "FACILITY_TYPE", nullable = false, length = 50)
    private String facilityType; // STUDY, SEMINAR, AUDITORIUM, GYM, EQUIPMENT

    @Column(name = "CAPACITY")
    private Integer capacity; // 수용 인원

    @Column(name = "LOCATION", length = 200)
    private String location;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Column(name = "DEFAULT_EQUIPMENT", length = 500)
    private String defaultEquipment; // CSV 형식: "빔프로젝터,화이트보드,마이크"

    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1; // 0: 비활성, 1: 활성

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // 생성자
    public Facility() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Facility(String facilityName, String facilityType, String description,
                   Integer capacity, String location, String defaultEquipment) {
        this();
        this.facilityName = facilityName;
        this.facilityType = facilityType;
        this.description = description;
        this.capacity = capacity;
        this.location = location;
        this.defaultEquipment = defaultEquipment;
    }

    // JPA 콜백
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getFacilityId() { return facilityId; }
    public void setFacilityId(Integer facilityId) { this.facilityId = facilityId; }

    public String getFacilityCode() { return facilityCode; }
    public void setFacilityCode(String facilityCode) { this.facilityCode = facilityCode; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDefaultEquipment() { return defaultEquipment; }
    public void setDefaultEquipment(String defaultEquipment) { this.defaultEquipment = defaultEquipment; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Facility{" +
                "facilityId=" + facilityId +
                ", facilityName='" + facilityName + '\'' +
                ", facilityType='" + facilityType + '\'' +
                ", capacity=" + capacity +
                ", location='" + location + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
```

### 3.2 Entity: `FacilityReservation.java`
```java
package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설물 예약 엔티티
 * FACILITY_RESERVATION_TBL 테이블과 매핑
 *
 * 상태 전이:
 * PENDING → APPROVED → COMPLETED
 * PENDING → REJECTED
 * PENDING/APPROVED → CANCELLED
 */
@Entity
@Table(name = "FACILITY_RESERVATION_TBL")
public class FacilityReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESERVATION_IDX")
    private Integer reservationId;

    @Column(name = "FACILITY_IDX", nullable = false)
    private Integer facilityId;

    @Column(name = "USER_IDX", nullable = false)
    private Integer userIdx; // UserTbl FK

    @Column(name = "USER_CODE", nullable = false, length = 255)
    private String userCode; // 조회 편의성

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "END_TIME", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "PARTY_SIZE")
    private Integer partySize; // 사용 인원

    @Column(name = "PURPOSE", columnDefinition = "TEXT")
    private String purpose; // 사용 목적

    @Column(name = "REQUESTED_EQUIPMENT", length = 500)
    private String requestedEquipment; // CSV 형식

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED

    @Column(name = "ADMIN_NOTE", columnDefinition = "TEXT")
    private String adminNote; // 관리자 메모

    @Column(name = "REJECTION_REASON", columnDefinition = "TEXT")
    private String rejectionReason; // 반려 사유

    @Column(name = "APPROVED_BY")
    private Integer approvedBy; // AdminTbl FK (선택)

    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt; // 승인/반려 처리 시간

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // 생성자
    public FacilityReservation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING"; // 기본값
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void approve(Integer adminId, String note) {
        this.status = "APPROVED";
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
        this.processedAt = LocalDateTime.now();
        this.adminNote = note;
    }

    public void reject(Integer adminId, String reason) {
        this.status = "REJECTED";
        this.approvedBy = adminId;
        this.processedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.processedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = "COMPLETED";
    }

    public boolean isExpired() {
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }

    // Getters and Setters
    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }

    public Integer getFacilityId() { return facilityId; }
    public void setFacilityId(Integer facilityId) { this.facilityId = facilityId; }

    public Integer getUserIdx() { return userIdx; }
    public void setUserIdx(Integer userIdx) { this.userIdx = userIdx; }

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRequestedEquipment() { return requestedEquipment; }
    public void setRequestedEquipment(String requestedEquipment) { this.requestedEquipment = requestedEquipment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "FacilityReservation{" +
                "reservationId=" + reservationId +
                ", facilityId=" + facilityId +
                ", userCode='" + userCode + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                '}';
    }
}
```

### 3.3 Entity: `FacilityReservationLog.java`
```java
package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설물 예약 감사 로그 엔티티
 * 모든 상태 전이 및 중요 이벤트 추적
 */
@Entity
@Table(name = "FACILITY_RESERVATION_LOG")
public class FacilityReservationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_IDX")
    private Integer logId;

    @Column(name = "RESERVATION_IDX", nullable = false)
    private Integer reservationId;

    @Column(name = "FACILITY_IDX", nullable = false)
    private Integer facilityId;

    @Column(name = "USER_CODE", nullable = false, length = 255)
    private String userCode;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType; // REQUESTED, APPROVED, REJECTED, CANCELLED, COMPLETED, SYSTEM_COMPLETED

    @Column(name = "ACTOR_TYPE", length = 20)
    private String actorType; // USER, ADMIN, SYSTEM

    @Column(name = "ACTOR_CODE", length = 255)
    private String actorCode; // userCode, adminId, "SYSTEM"

    @Column(name = "PAYLOAD", columnDefinition = "TEXT")
    private String payload; // JSON 형식 추가 정보

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    // 생성자
    public FacilityReservationLog() {
        this.createdAt = LocalDateTime.now();
    }

    public FacilityReservationLog(Integer reservationId, Integer facilityId, String userCode,
                                  String eventType, String actorType, String actorCode) {
        this();
        this.reservationId = reservationId;
        this.facilityId = facilityId;
        this.userCode = userCode;
        this.eventType = eventType;
        this.actorType = actorType;
        this.actorCode = actorCode;
    }

    // Getters and Setters
    public Integer getLogId() { return logId; }
    public void setLogId(Integer logId) { this.logId = logId; }

    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }

    public Integer getFacilityId() { return facilityId; }
    public void setFacilityId(Integer facilityId) { this.facilityId = facilityId; }

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }

    public String getActorCode() { return actorCode; }
    public void setActorCode(String actorCode) { this.actorCode = actorCode; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### 3.4 데이터베이스 스키마 (DDL)

```sql
-- 시설물 테이블
CREATE TABLE FACILITY_TBL (
    FACILITY_IDX INT PRIMARY KEY AUTO_INCREMENT,
    FACILITY_CODE VARCHAR(50),
    FACILITY_NAME VARCHAR(100) NOT NULL,
    FACILITY_TYPE VARCHAR(50) NOT NULL,
    CAPACITY INT,
    LOCATION VARCHAR(200),
    DESCRIPTION TEXT,
    DEFAULT_EQUIPMENT VARCHAR(500),
    IS_ACTIVE INT NOT NULL DEFAULT 1,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_active (FACILITY_TYPE, IS_ACTIVE),
    INDEX idx_name (FACILITY_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 예약 테이블
CREATE TABLE FACILITY_RESERVATION_TBL (
    RESERVATION_IDX INT PRIMARY KEY AUTO_INCREMENT,
    FACILITY_IDX INT NOT NULL,
    USER_IDX INT NOT NULL,
    USER_CODE VARCHAR(255) NOT NULL,
    START_TIME DATETIME NOT NULL,
    END_TIME DATETIME NOT NULL,
    PARTY_SIZE INT,
    PURPOSE TEXT,
    REQUESTED_EQUIPMENT VARCHAR(500),
    STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADMIN_NOTE TEXT,
    REJECTION_REASON TEXT,
    APPROVED_BY INT,
    APPROVED_AT DATETIME,
    PROCESSED_AT DATETIME,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (FACILITY_IDX) REFERENCES FACILITY_TBL(FACILITY_IDX),
    FOREIGN KEY (USER_IDX) REFERENCES USER_TBL(USER_IDX),
    INDEX idx_facility_time (FACILITY_IDX, START_TIME, END_TIME),
    INDEX idx_user_status (USER_CODE, STATUS),
    INDEX idx_status (STATUS),
    INDEX idx_status_time (STATUS, END_TIME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 감사 로그 테이블
CREATE TABLE FACILITY_RESERVATION_LOG (
    LOG_IDX INT PRIMARY KEY AUTO_INCREMENT,
    RESERVATION_IDX INT NOT NULL,
    FACILITY_IDX INT NOT NULL,
    USER_CODE VARCHAR(255) NOT NULL,
    EVENT_TYPE VARCHAR(50) NOT NULL,
    ACTOR_TYPE VARCHAR(20),
    ACTOR_CODE VARCHAR(255),
    PAYLOAD TEXT,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (RESERVATION_IDX) REFERENCES FACILITY_RESERVATION_TBL(RESERVATION_IDX),
    INDEX idx_reservation (RESERVATION_IDX),
    INDEX idx_user (USER_CODE),
    INDEX idx_facility (FACILITY_IDX),
    INDEX idx_event (EVENT_TYPE, CREATED_AT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 4. Repository 계층

### 4.1 `FacilityRepository.java`
```java
package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Integer> {

    // 활성 시설물 전체 조회
    List<Facility> findByIsActiveOrderByFacilityName(Integer isActive);

    // 타입별 조회
    List<Facility> findByFacilityTypeAndIsActiveOrderByFacilityName(String facilityType, Integer isActive);

    // 이름 검색
    List<Facility> findByFacilityNameContainingIgnoreCaseAndIsActive(String keyword, Integer isActive);

    // ID로 활성 시설물 조회
    Optional<Facility> findByFacilityIdAndIsActive(Integer facilityId, Integer isActive);
}
```

### 4.2 `FacilityReservationRepository.java`
```java
package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservation, Integer> {

    /**
     * 시간대 중복 체크 (예약 생성 시 사용)
     * PENDING, APPROVED 상태의 예약 중 시간대가 겹치는 것 조회
     */
    @Query("SELECT fr FROM FacilityReservation fr WHERE fr.facilityId = :facilityId " +
           "AND fr.status IN ('PENDING', 'APPROVED') " +
           "AND ((fr.startTime < :endTime AND fr.endTime > :startTime))")
    List<FacilityReservation> findConflictingReservations(
        @Param("facilityId") Integer facilityId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 사용자별 예약 목록 (상태별 필터)
     */
    List<FacilityReservation> findByUserCodeAndStatusInOrderByStartTimeDesc(
        String userCode, List<String> statuses
    );

    /**
     * 사용자의 활성 예약 개수 (PENDING, APPROVED)
     */
    long countByUserCodeAndStatusIn(String userCode, List<String> statuses);

    /**
     * 특정 시설물의 예약 목록
     */
    List<FacilityReservation> findByFacilityIdAndStatusInOrderByStartTime(
        Integer facilityId, List<String> statuses
    );

    /**
     * 특정 시설물의 특정 날짜 예약 목록
     */
    @Query("SELECT fr FROM FacilityReservation fr WHERE fr.facilityId = :facilityId " +
           "AND fr.status IN :statuses " +
           "AND DATE(fr.startTime) = DATE(:targetDate) " +
           "ORDER BY fr.startTime")
    List<FacilityReservation> findByFacilityIdAndDateAndStatuses(
        @Param("facilityId") Integer facilityId,
        @Param("targetDate") LocalDateTime targetDate,
        @Param("statuses") List<String> statuses
    );

    /**
     * 만료된 예약 조회 (자동 완료 처리용)
     */
    @Query("SELECT fr FROM FacilityReservation fr WHERE fr.status = 'APPROVED' " +
           "AND fr.endTime < :currentTime")
    List<FacilityReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 승인 대기 목록
     */
    List<FacilityReservation> findByStatusOrderByCreatedAtAsc(String status);

    /**
     * 관리자용 전체 목록 (페이징 가능하도록 추후 확장)
     */
    List<FacilityReservation> findAllByOrderByCreatedAtDesc();

    /**
     * 통계: 오늘 예약 수
     */
    @Query("SELECT COUNT(fr) FROM FacilityReservation fr " +
           "WHERE DATE(fr.startTime) = DATE(:today) " +
           "AND fr.status IN :statuses")
    long countTodayReservations(@Param("today") LocalDateTime today, @Param("statuses") List<String> statuses);

    /**
     * 통계: 기간별 예약 수
     */
    @Query("SELECT COUNT(fr) FROM FacilityReservation fr " +
           "WHERE fr.createdAt >= :startDate AND fr.createdAt < :endDate " +
           "AND fr.status IN :statuses")
    long countReservationsByPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("statuses") List<String> statuses
    );
}
```

### 4.3 `FacilityReservationLogRepository.java`
```java
package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityReservationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityReservationLogRepository extends JpaRepository<FacilityReservationLog, Integer> {

    // 특정 예약의 로그 조회 (시간순)
    List<FacilityReservationLog> findByReservationIdOrderByCreatedAtAsc(Integer reservationId);

    // 사용자별 로그 조회
    List<FacilityReservationLog> findByUserCodeOrderByCreatedAtDesc(String userCode);

    // 시설물별 로그 조회
    List<FacilityReservationLog> findByFacilityIdOrderByCreatedAtDesc(Integer facilityId);

    // 이벤트 타입별 조회
    List<FacilityReservationLog> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
```

---

## 5. DTO 설계

### 5.1 Request DTOs

#### `FacilityListRequestDto.java`
```java
package BlueCrab.com.example.dto;

public class FacilityListRequestDto {
    private String facilityType; // STUDY, SEMINAR, AUDITORIUM, GYM, EQUIPMENT (선택)
    private String keyword; // 검색어 (선택)

    // Constructors, Getters, Setters
    public FacilityListRequestDto() {}

    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
```

#### `FacilityAvailabilityRequestDto.java`
```java
package BlueCrab.com.example.dto;

import java.time.LocalDate;

public class FacilityAvailabilityRequestDto {
    private LocalDate reserveDate; // 조회할 날짜

    public FacilityAvailabilityRequestDto() {}

    public LocalDate getReserveDate() { return reserveDate; }
    public void setReserveDate(LocalDate reserveDate) { this.reserveDate = reserveDate; }
}
```

#### `FacilityReservationRequestDto.java`
```java
package BlueCrab.com.example.dto;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class FacilityReservationRequestDto {

    @NotNull(message = "시설물 ID는 필수입니다.")
    private Integer facilityId;

    @NotNull(message = "시작 시간은 필수입니다.")
    private LocalDateTime startTime;

    @NotNull(message = "종료 시간은 필수입니다.")
    private LocalDateTime endTime;

    private Integer partySize; // 사용 인원

    private String purpose; // 사용 목적

    private String requestedEquipment; // 요청 장비 (CSV)

    // Constructors, Getters, Setters
    public FacilityReservationRequestDto() {}

    public Integer getFacilityId() { return facilityId; }
    public void setFacilityId(Integer facilityId) { this.facilityId = facilityId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRequestedEquipment() { return requestedEquipment; }
    public void setRequestedEquipment(String requestedEquipment) { this.requestedEquipment = requestedEquipment; }
}
```

#### `ReservationCancelRequestDto.java`
```java
package BlueCrab.com.example.dto;

import javax.validation.constraints.NotNull;

public class ReservationCancelRequestDto {

    @NotNull(message = "예약 ID는 필수입니다.")
    private Integer reservationId;

    public ReservationCancelRequestDto() {}

    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }
}
```

#### `AdminReservationDecisionRequestDto.java`
```java
package BlueCrab.com.example.dto;

public class AdminReservationDecisionRequestDto {
    private String note; // 관리자 메모 (승인 시)
    private String rejectionReason; // 반려 사유 (반려 시)

    public AdminReservationDecisionRequestDto() {}

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
```

### 5.2 Response DTOs

#### `FacilityDto.java`
```java
package BlueCrab.com.example.dto;

public class FacilityDto {
    private Integer facilityId;
    private String facilityCode;
    private String facilityName;
    private String facilityType;
    private String description;
    private Integer capacity;
    private String location;
    private String defaultEquipment;

    // Constructors, Getters, Setters
    public FacilityDto() {}

    public FacilityDto(Integer facilityId, String facilityName, String facilityType,
                       String description, Integer capacity, String location, String defaultEquipment) {
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.facilityType = facilityType;
        this.description = description;
        this.capacity = capacity;
        this.location = location;
        this.defaultEquipment = defaultEquipment;
    }

    // Getters and Setters
    public Integer getFacilityId() { return facilityId; }
    public void setFacilityId(Integer facilityId) { this.facilityId = facilityId; }

    public String getFacilityCode() { return facilityCode; }
    public void setFacilityCode(String facilityCode) { this.facilityCode = facilityCode; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDefaultEquipment() { return defaultEquipment; }
    public void setDefaultEquipment(String defaultEquipment) { this.defaultEquipment = defaultEquipment; }
}
```

#### `TimeSlotDto.java`
```java
package BlueCrab.com.example.dto;

import java.time.LocalTime;

public class TimeSlotDto {
    private LocalTime startTime; // 09:00
    private LocalTime endTime;   // 10:00
    private String status;       // available, reserved, pending

    public TimeSlotDto() {}

    public TimeSlotDto(LocalTime startTime, LocalTime endTime, String status) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters and Setters
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

#### `FacilityDetailDto.java`
```java
package BlueCrab.com.example.dto;

import java.util.List;

public class FacilityDetailDto extends FacilityDto {
    private List<ReservationSummaryDto> upcomingReservations;

    public FacilityDetailDto() {}

    public List<ReservationSummaryDto> getUpcomingReservations() { return upcomingReservations; }
    public void setUpcomingReservations(List<ReservationSummaryDto> upcomingReservations) {
        this.upcomingReservations = upcomingReservations;
    }
}
```

#### `ReservationSummaryDto.java`
```java
package BlueCrab.com.example.dto;

import java.time.LocalDateTime;

public class ReservationSummaryDto {
    private Integer reservationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public ReservationSummaryDto() {}

    public ReservationSummaryDto(Integer reservationId, LocalDateTime startTime,
                                LocalDateTime endTime, String status) {
        this.reservationId = reservationId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters and Setters
    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

#### `MyReservationDto.java`
```java
package BlueCrab.com.example.dto;

import java.time.LocalDateTime;

public class MyReservationDto {
    private Integer reservationId;
    private FacilityDto facility;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer partySize;
    private String purpose;
    private String requestedEquipment;
    private String status;
    private String adminNote;
    private String rejectionReason;
    private LocalDateTime createdAt;

    // Constructors, Getters, Setters
    public MyReservationDto() {}

    // Getters and Setters (생략 - 모든 필드에 대해 생성)
    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }

    public FacilityDto getFacility() { return facility; }
    public void setFacility(FacilityDto facility) { this.facility = facility; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRequestedEquipment() { return requestedEquipment; }
    public void setRequestedEquipment(String requestedEquipment) { this.requestedEquipment = requestedEquipment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

#### `MyReservationListDto.java`
```java
package BlueCrab.com.example.dto;

import java.util.List;

public class MyReservationListDto {
    private List<MyReservationDto> upcomingReservations; // PENDING, APPROVED
    private List<MyReservationDto> pastReservations;     // COMPLETED, REJECTED, CANCELLED

    public MyReservationListDto() {}

    public MyReservationListDto(List<MyReservationDto> upcomingReservations,
                                List<MyReservationDto> pastReservations) {
        this.upcomingReservations = upcomingReservations;
        this.pastReservations = pastReservations;
    }

    public List<MyReservationDto> getUpcomingReservations() { return upcomingReservations; }
    public void setUpcomingReservations(List<MyReservationDto> upcomingReservations) {
        this.upcomingReservations = upcomingReservations;
    }

    public List<MyReservationDto> getPastReservations() { return pastReservations; }
    public void setPastReservations(List<MyReservationDto> pastReservations) {
        this.pastReservations = pastReservations;
    }
}
```

#### `AdminReservationStatsDto.java`
```java
package BlueCrab.com.example.dto;

public class AdminReservationStatsDto {
    private Long pendingCount;     // 승인 대기
    private Long todayTotal;       // 오늘 예약
    private Long todayInUse;       // 오늘 사용 중
    private Long weekTotal;        // 이번 주 예약
    private Long monthTotal;       // 이번 달 예약

    public AdminReservationStatsDto() {}

    public AdminReservationStatsDto(Long pendingCount, Long todayTotal, Long todayInUse,
                                    Long weekTotal, Long monthTotal) {
        this.pendingCount = pendingCount;
        this.todayTotal = todayTotal;
        this.todayInUse = todayInUse;
        this.weekTotal = weekTotal;
        this.monthTotal = monthTotal;
    }

    // Getters and Setters
    public Long getPendingCount() { return pendingCount; }
    public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }

    public Long getTodayTotal() { return todayTotal; }
    public void setTodayTotal(Long todayTotal) { this.todayTotal = todayTotal; }

    public Long getTodayInUse() { return todayInUse; }
    public void setTodayInUse(Long todayInUse) { this.todayInUse = todayInUse; }

    public Long getWeekTotal() { return weekTotal; }
    public void setWeekTotal(Long weekTotal) { this.weekTotal = weekTotal; }

    public Long getMonthTotal() { return monthTotal; }
    public void setMonthTotal(Long monthTotal) { this.monthTotal = monthTotal; }
}
```

---

## 6. Service 로직

### 6.1 `FacilityService.java`

```java
package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.Facility;
import BlueCrab.com.example.entity.FacilityReservation;
import BlueCrab.com.example.repository.FacilityRepository;
import BlueCrab.com.example.repository.FacilityReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시설물 관리 서비스
 */
@Service
@Transactional(readOnly = true)
public class FacilityService {

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private FacilityReservationRepository reservationRepository;

    /**
     * 시설물 목록 조회
     */
    public List<FacilityDto> getFacilities(String facilityType, String keyword) {
        List<Facility> facilities;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색어가 있는 경우
            facilities = facilityRepository.findByFacilityNameContainingIgnoreCaseAndIsActive(keyword, 1);
        } else if (facilityType != null && !facilityType.trim().isEmpty()) {
            // 타입 필터만 있는 경우
            facilities = facilityRepository.findByFacilityTypeAndIsActiveOrderByFacilityName(facilityType, 1);
        } else {
            // 전체 조회
            facilities = facilityRepository.findByIsActiveOrderByFacilityName(1);
        }

        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 시설물 상세 조회
     */
    public FacilityDetailDto getFacilityDetail(Integer facilityId) {
        Facility facility = facilityRepository.findByFacilityIdAndIsActive(facilityId, 1)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 사용할 수 없는 시설입니다."));

        FacilityDetailDto detailDto = new FacilityDetailDto();
        detailDto.setFacilityId(facility.getFacilityId());
        detailDto.setFacilityCode(facility.getFacilityCode());
        detailDto.setFacilityName(facility.getFacilityName());
        detailDto.setFacilityType(facility.getFacilityType());
        detailDto.setDescription(facility.getDescription());
        detailDto.setCapacity(facility.getCapacity());
        detailDto.setLocation(facility.getLocation());
        detailDto.setDefaultEquipment(facility.getDefaultEquipment());

        // 다가오는 예약 조회 (오늘 이후, APPROVED 상태)
        LocalDateTime now = LocalDateTime.now();
        List<FacilityReservation> upcomingReservations = reservationRepository
                .findByFacilityIdAndStatusInOrderByStartTime(
                        facilityId,
                        Arrays.asList("APPROVED")
                )
                .stream()
                .filter(r -> r.getStartTime().isAfter(now))
                .limit(5)
                .collect(Collectors.toList());

        List<ReservationSummaryDto> summaries = upcomingReservations.stream()
                .map(r -> new ReservationSummaryDto(
                        r.getReservationId(),
                        r.getStartTime(),
                        r.getEndTime(),
                        r.getStatus()
                ))
                .collect(Collectors.toList());

        detailDto.setUpcomingReservations(summaries);

        return detailDto;
    }

    /**
     * 예약 가능 시간대 조회 (특정 날짜)
     */
    public List<TimeSlotDto> getAvailability(Integer facilityId, LocalDate targetDate) {
        // 1. 시설물 확인
        Facility facility = facilityRepository.findByFacilityIdAndIsActive(facilityId, 1)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 사용할 수 없는 시설입니다."));

        // 2. 해당 날짜의 예약 조회
        LocalDateTime targetDateTime = targetDate.atStartOfDay();
        List<FacilityReservation> reservations = reservationRepository
                .findByFacilityIdAndDateAndStatuses(
                        facilityId,
                        targetDateTime,
                        Arrays.asList("PENDING", "APPROVED")
                );

        // 3. 시간 슬롯 생성 (09:00 ~ 21:00, 1시간 단위)
        List<TimeSlotDto> slots = new ArrayList<>();
        for (int hour = 9; hour < 21; hour++) {
            LocalTime startTime = LocalTime.of(hour, 0);
            LocalTime endTime = LocalTime.of(hour + 1, 0);

            LocalDateTime slotStart = LocalDateTime.of(targetDate, startTime);
            LocalDateTime slotEnd = LocalDateTime.of(targetDate, endTime);

            // 4. 해당 슬롯의 상태 결정
            String status = "available";
            for (FacilityReservation reservation : reservations) {
                if (isOverlapping(slotStart, slotEnd, reservation.getStartTime(), reservation.getEndTime())) {
                    status = reservation.getStatus().equals("PENDING") ? "pending" : "reserved";
                    break;
                }
            }

            slots.add(new TimeSlotDto(startTime, endTime, status));
        }

        return slots;
    }

    /**
     * 시간대 겹침 확인
     */
    private boolean isOverlapping(LocalDateTime start1, LocalDateTime end1,
                                  LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Entity to DTO 변환
     */
    private FacilityDto convertToDto(Facility facility) {
        return new FacilityDto(
                facility.getFacilityId(),
                facility.getFacilityName(),
                facility.getFacilityType(),
                facility.getDescription(),
                facility.getCapacity(),
                facility.getLocation(),
                facility.getDefaultEquipment()
        );
    }
}
```

### 6.2 `FacilityReservationService.java`

```java
package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.Facility;
import BlueCrab.com.example.entity.FacilityReservation;
import BlueCrab.com.example.entity.FacilityReservationLog;
import BlueCrab.com.example.repository.FacilityRepository;
import BlueCrab.com.example.repository.FacilityReservationLogRepository;
import BlueCrab.com.example.repository.FacilityReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시설물 예약 서비스
 */
@Service
@Transactional(readOnly = true)
public class FacilityReservationService {

    private static final Logger logger = LoggerFactory.getLogger(FacilityReservationService.class);

    // 정책 상수
    private static final int MAX_RESERVATION_DAYS_AHEAD = 14; // 14일 이내만 예약 가능
    private static final int MIN_RESERVATION_MINUTES = 60;    // 최소 1시간
    private static final int MAX_RESERVATION_MINUTES = 240;   // 최대 4시간
    private static final int MAX_ACTIVE_RESERVATIONS_PER_USER = 5; // 1인당 최대 5개
    private static final int CANCELLATION_DEADLINE_HOURS = 2; // 시작 2시간 전까지 취소 가능

    @Autowired
    private FacilityReservationRepository reservationRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private FacilityReservationLogRepository logRepository;

    /**
     * 예약 생성
     */
    @Transactional
    public MyReservationDto createReservation(FacilityReservationRequestDto request,
                                               String userCode, Integer userIdx) {
        logger.info("예약 생성 요청 - 사용자: {}, 시설: {}", userCode, request.getFacilityId());

        // 1. 사용자의 활성 예약 개수 확인
        long activeCount = reservationRepository.countByUserCodeAndStatusIn(
                userCode, Arrays.asList("PENDING", "APPROVED")
        );

        if (activeCount >= MAX_ACTIVE_RESERVATIONS_PER_USER) {
            throw new IllegalStateException(
                    "최대 " + MAX_ACTIVE_RESERVATIONS_PER_USER + "개까지 예약 가능합니다."
            );
        }

        // 2. 시설물 확인
        Facility facility = facilityRepository.findByFacilityIdAndIsActive(request.getFacilityId(), 1)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 사용할 수 없는 시설입니다."));

        // 3. 예약 시간 검증
        validateReservationTime(request.getStartTime(), request.getEndTime());

        // 4. 인원 수 검증
        if (facility.getCapacity() != null && request.getPartySize() != null &&
            request.getPartySize() > facility.getCapacity()) {
            throw new IllegalArgumentException(
                    "최대 " + facility.getCapacity() + "명까지 수용 가능합니다."
            );
        }

        // 5. 시간대 중복 확인
        List<FacilityReservation> conflicts = reservationRepository.findConflictingReservations(
                request.getFacilityId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("해당 시간대에 이미 예약이 있습니다.");
        }

        // 6. 예약 생성
        FacilityReservation reservation = new FacilityReservation();
        reservation.setFacilityId(request.getFacilityId());
        reservation.setUserIdx(userIdx);
        reservation.setUserCode(userCode);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setPurpose(request.getPurpose());
        reservation.setRequestedEquipment(request.getRequestedEquipment());
        reservation.setStatus("PENDING"); // 관리자 승인 대기

        FacilityReservation saved = reservationRepository.save(reservation);

        // 7. 로그 기록
        FacilityReservationLog log = new FacilityReservationLog(
                saved.getReservationId(),
                saved.getFacilityId(),
                userCode,
                "REQUESTED",
                "USER",
                userCode
        );
        logRepository.save(log);

        logger.info("예약 생성 완료 - 예약ID: {}, 상태: PENDING", saved.getReservationId());

        // 8. DTO 변환
        return convertToMyReservationDto(saved, facility);
    }

    /**
     * 예약 취소
     */
    @Transactional
    public void cancelReservation(Integer reservationId, String userCode) {
        logger.info("예약 취소 요청 - 예약ID: {}, 사용자: {}", reservationId, userCode);

        // 1. 예약 조회 및 권한 확인
        FacilityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (!reservation.getUserCode().equals(userCode)) {
            throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
        }

        // 2. 취소 가능 상태 확인
        if (Arrays.asList("CANCELLED", "COMPLETED", "REJECTED").contains(reservation.getStatus())) {
            throw new IllegalStateException("취소할 수 없는 예약입니다.");
        }

        // 3. 취소 가능 시간 확인 (APPROVED 상태인 경우만)
        if ("APPROVED".equals(reservation.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = reservation.getStartTime().minusHours(CANCELLATION_DEADLINE_HOURS);

            if (now.isAfter(deadline)) {
                throw new IllegalStateException(
                        "시작 " + CANCELLATION_DEADLINE_HOURS + "시간 전까지만 취소 가능합니다."
                );
            }
        }

        // 4. 예약 취소
        reservation.cancel();
        reservationRepository.save(reservation);

        // 5. 로그 기록
        FacilityReservationLog log = new FacilityReservationLog(
                reservationId,
                reservation.getFacilityId(),
                userCode,
                "CANCELLED",
                "USER",
                userCode
        );
        logRepository.save(log);

        logger.info("예약 취소 완료 - 예약ID: {}", reservationId);
    }

    /**
     * 내 예약 목록 조회
     */
    public MyReservationListDto getMyReservations(String userCode) {
        // 진행중 예약 (PENDING, APPROVED)
        List<FacilityReservation> upcomingReservations = reservationRepository
                .findByUserCodeAndStatusInOrderByStartTimeDesc(
                        userCode,
                        Arrays.asList("PENDING", "APPROVED")
                );

        // 완료된 예약 (COMPLETED, REJECTED, CANCELLED)
        List<FacilityReservation> pastReservations = reservationRepository
                .findByUserCodeAndStatusInOrderByStartTimeDesc(
                        userCode,
                        Arrays.asList("COMPLETED", "REJECTED", "CANCELLED")
                );

        List<MyReservationDto> upcomingDtos = upcomingReservations.stream()
                .map(this::convertToMyReservationDtoWithFacility)
                .collect(Collectors.toList());

        List<MyReservationDto> pastDtos = pastReservations.stream()
                .map(this::convertToMyReservationDtoWithFacility)
                .collect(Collectors.toList());

        return new MyReservationListDto(upcomingDtos, pastDtos);
    }

    /**
     * 만료된 예약 자동 완료 처리 (스케줄러에서 호출)
     */
    @Transactional
    public void completeExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<FacilityReservation> expiredReservations = reservationRepository.findExpiredReservations(now);

        for (FacilityReservation reservation : expiredReservations) {
            reservation.complete();

            // 로그 기록
            FacilityReservationLog log = new FacilityReservationLog(
                    reservation.getReservationId(),
                    reservation.getFacilityId(),
                    reservation.getUserCode(),
                    "SYSTEM_COMPLETED",
                    "SYSTEM",
                    "AUTO_SCHEDULER"
            );
            logRepository.save(log);
        }

        if (!expiredReservations.isEmpty()) {
            reservationRepository.saveAll(expiredReservations);
            logger.info("만료된 예약 {}건 자동 완료 처리", expiredReservations.size());
        }
    }

    /**
     * 예약 시간 유효성 검증
     */
    private void validateReservationTime(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxReservationDate = now.plusDays(MAX_RESERVATION_DAYS_AHEAD);

        // 과거 시간 체크
        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException("과거 시간은 예약할 수 없습니다.");
        }

        // 최대 예약 가능 일수 체크
        if (startTime.isAfter(maxReservationDate)) {
            throw new IllegalArgumentException(
                    MAX_RESERVATION_DAYS_AHEAD + "일 이내만 예약 가능합니다."
            );
        }

        // 종료 시간이 시작 시간보다 이후인지 확인
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        // 예약 시간 길이 체크
        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (durationMinutes < MIN_RESERVATION_MINUTES) {
            throw new IllegalArgumentException(
                    "최소 " + MIN_RESERVATION_MINUTES + "분 이상 예약해야 합니다."
            );
        }
        if (durationMinutes > MAX_RESERVATION_MINUTES) {
            throw new IllegalArgumentException(
                    "최대 " + MAX_RESERVATION_MINUTES + "분까지 예약 가능합니다."
            );
        }
    }

    /**
     * Entity to DTO 변환 (Facility 포함)
     */
    private MyReservationDto convertToMyReservationDtoWithFacility(FacilityReservation reservation) {
        Facility facility = facilityRepository.findById(reservation.getFacilityId())
                .orElse(null);

        return convertToMyReservationDto(reservation, facility);
    }

    /**
     * Entity to DTO 변환
     */
    private MyReservationDto convertToMyReservationDto(FacilityReservation reservation, Facility facility) {
        MyReservationDto dto = new MyReservationDto();
        dto.setReservationId(reservation.getReservationId());
        dto.setStartTime(reservation.getStartTime());
        dto.setEndTime(reservation.getEndTime());
        dto.setPartySize(reservation.getPartySize());
        dto.setPurpose(reservation.getPurpose());
        dto.setRequestedEquipment(reservation.getRequestedEquipment());
        dto.setStatus(reservation.getStatus());
        dto.setAdminNote(reservation.getAdminNote());
        dto.setRejectionReason(reservation.getRejectionReason());
        dto.setCreatedAt(reservation.getCreatedAt());

        if (facility != null) {
            FacilityDto facilityDto = new FacilityDto(
                    facility.getFacilityId(),
                    facility.getFacilityName(),
                    facility.getFacilityType(),
                    facility.getDescription(),
                    facility.getCapacity(),
                    facility.getLocation(),
                    facility.getDefaultEquipment()
            );
            dto.setFacility(facilityDto);
        }

        return dto;
    }
}
```

### 6.3 `AdminFacilityReservationService.java`

```java
package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.AdminReservationStatsDto;
import BlueCrab.com.example.dto.MyReservationDto;
import BlueCrab.com.example.entity.FacilityReservation;
import BlueCrab.com.example.entity.FacilityReservationLog;
import BlueCrab.com.example.repository.FacilityReservationLogRepository;
import BlueCrab.com.example.repository.FacilityReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 시설물 예약 서비스
 */
@Service
@Transactional(readOnly = true)
public class AdminFacilityReservationService {

    private static final Logger logger = LoggerFactory.getLogger(AdminFacilityReservationService.class);

    @Autowired
    private FacilityReservationRepository reservationRepository;

    @Autowired
    private FacilityReservationLogRepository logRepository;

    @Autowired
    private FacilityReservationService reservationService; // DTO 변환 재사용

    /**
     * 승인 대기 목록 조회
     */
    public List<MyReservationDto> getPendingReservations() {
        List<FacilityReservation> pending = reservationRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        return pending.stream()
                .map(r -> reservationService.convertToMyReservationDtoWithFacility(r))
                .collect(Collectors.toList());
    }

    /**
     * 전체 예약 목록 조회 (관리자용)
     */
    public List<MyReservationDto> getAllReservations() {
        List<FacilityReservation> allReservations = reservationRepository.findAllByOrderByCreatedAtDesc();

        return allReservations.stream()
                .map(r -> reservationService.convertToMyReservationDtoWithFacility(r))
                .collect(Collectors.toList());
    }

    /**
     * 예약 승인
     */
    @Transactional
    public void approveReservation(Integer reservationId, Integer adminId, String adminCode, String note) {
        logger.info("예약 승인 요청 - 예약ID: {}, 관리자: {}", reservationId, adminCode);

        FacilityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalStateException("승인 대기 중인 예약만 승인할 수 있습니다.");
        }

        // 승인 처리
        reservation.approve(adminId, note);
        reservationRepository.save(reservation);

        // 로그 기록
        FacilityReservationLog log = new FacilityReservationLog(
                reservationId,
                reservation.getFacilityId(),
                reservation.getUserCode(),
                "APPROVED",
                "ADMIN",
                adminCode
        );
        log.setPayload(note);
        logRepository.save(log);

        logger.info("예약 승인 완료 - 예약ID: {}", reservationId);

        // TODO: FCM 푸시 알림 전송 (선택)
    }

    /**
     * 예약 반려
     */
    @Transactional
    public void rejectReservation(Integer reservationId, Integer adminId, String adminCode, String reason) {
        logger.info("예약 반려 요청 - 예약ID: {}, 관리자: {}", reservationId, adminCode);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("반려 사유는 필수입니다.");
        }

        FacilityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalStateException("승인 대기 중인 예약만 반려할 수 있습니다.");
        }

        // 반려 처리
        reservation.reject(adminId, reason);
        reservationRepository.save(reservation);

        // 로그 기록
        FacilityReservationLog log = new FacilityReservationLog(
                reservationId,
                reservation.getFacilityId(),
                reservation.getUserCode(),
                "REJECTED",
                "ADMIN",
                adminCode
        );
        log.setPayload(reason);
        logRepository.save(log);

        logger.info("예약 반려 완료 - 예약ID: {}", reservationId);

        // TODO: FCM 푸시 알림 전송 (선택)
    }

    /**
     * 대시보드 통계 조회
     */
    public AdminReservationStatsDto getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();

        // 승인 대기
        long pendingCount = reservationRepository.countByStatusIn(Arrays.asList("PENDING"));

        // 오늘 예약 (생성 기준)
        LocalDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        long todayTotal = reservationRepository.countReservationsByPeriod(
                startOfToday, startOfTomorrow, Arrays.asList("PENDING", "APPROVED", "COMPLETED")
        );

        // 오늘 사용 중 (시작 시간 기준)
        long todayInUse = reservationRepository.countTodayReservations(
                now, Arrays.asList("APPROVED")
        );

        // 이번 주 예약
        LocalDateTime startOfWeek = now.truncatedTo(ChronoUnit.DAYS).minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfNextWeek = startOfWeek.plusWeeks(1);
        long weekTotal = reservationRepository.countReservationsByPeriod(
                startOfWeek, startOfNextWeek, Arrays.asList("PENDING", "APPROVED", "COMPLETED")
        );

        // 이번 달 예약
        LocalDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        long monthTotal = reservationRepository.countReservationsByPeriod(
                startOfMonth, startOfNextMonth, Arrays.asList("PENDING", "APPROVED", "COMPLETED")
        );

        return new AdminReservationStatsDto(pendingCount, todayTotal, todayInUse, weekTotal, monthTotal);
    }
}
```

---

## 7. Controller 설계

### 7.1 `FacilityController.java`

```java
package BlueCrab.com.example.controller;

import BlueCrab.com.example.annotation.RateLimit;
import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.service.FacilityService;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

/**
 * 시설물 정보 조회 컨트롤러
 */
@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private static final Logger logger = LoggerFactory.getLogger(FacilityController.class);

    @Autowired
    private FacilityService facilityService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 시설물 목록 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 10, message = "시설물 조회가 너무 빈번합니다.")
    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getFacilities(
            HttpServletRequest request,
            @RequestBody(required = false) FacilityListRequestDto requestDto) {

        try {
            // JWT 검증
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            String facilityType = requestDto != null ? requestDto.getFacilityType() : null;
            String keyword = requestDto != null ? requestDto.getKeyword() : null;

            logger.info("시설물 목록 조회 - 타입: {}, 검색어: {}", facilityType, keyword);

            List<FacilityDto> facilities = facilityService.getFacilities(facilityType, keyword);

            return ResponseEntity.ok(new ApiResponse<>(true, "시설물 목록을 조회했습니다.", facilities));

        } catch (Exception e) {
            logger.error("시설물 목록 조회 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "목록 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 시설물 상세 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 10, message = "상세 조회가 너무 빈번합니다.")
    @PostMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<FacilityDetailDto>> getFacilityDetail(
            HttpServletRequest request,
            @PathVariable Integer id) {

        try {
            // JWT 검증
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            logger.info("시설물 상세 조회 - ID: {}", id);

            FacilityDetailDto detail = facilityService.getFacilityDetail(id);

            return ResponseEntity.ok(new ApiResponse<>(true, "시설물 상세 정보를 조회했습니다.", detail));

        } catch (IllegalArgumentException e) {
            logger.warn("시설물 조회 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "NOT_FOUND"));

        } catch (Exception e) {
            logger.error("시설물 상세 조회 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "상세 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 예약 가능 시간대 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 15, message = "시간대 조회가 너무 빈번합니다.")
    @PostMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<TimeSlotDto>>> getAvailability(
            HttpServletRequest request,
            @PathVariable Integer id,
            @RequestBody FacilityAvailabilityRequestDto requestDto) {

        try {
            // JWT 검증
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            LocalDate targetDate = requestDto.getReserveDate();
            if (targetDate == null) {
                targetDate = LocalDate.now();
            }

            logger.info("예약 가능 시간대 조회 - 시설ID: {}, 날짜: {}", id, targetDate);

            List<TimeSlotDto> slots = facilityService.getAvailability(id, targetDate);

            return ResponseEntity.ok(new ApiResponse<>(true, "예약 가능 시간대를 조회했습니다.", slots));

        } catch (IllegalArgumentException e) {
            logger.warn("시간대 조회 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "INVALID_REQUEST"));

        } catch (Exception e) {
            logger.error("시간대 조회 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "시간대 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### 7.2 `FacilityReservationController.java`

```java
package BlueCrab.com.example.controller;

import BlueCrab.com.example.annotation.RateLimit;
import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.FacilityReservationService;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

/**
 * 시설물 예약 컨트롤러 (사용자용)
 */
@RestController
@RequestMapping("/api/facility-reservations")
public class FacilityReservationController {

    private static final Logger logger = LoggerFactory.getLogger(FacilityReservationController.class);

    @Autowired
    private FacilityReservationService reservationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserTblRepository userTblRepository;

    /**
     * 예약 생성
     */
    @RateLimit(timeWindow = 60, maxRequests = 3, message = "예약 요청이 너무 빈번합니다.")
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<MyReservationDto>> createReservation(
            HttpServletRequest request,
            @Valid @RequestBody FacilityReservationRequestDto requestDto) {

        try {
            // JWT 검증 및 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            Integer userId = jwtUtil.extractUserId(token);
            Optional<UserTbl> userOpt = userTblRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "사용자 정보를 찾을 수 없습니다.", null, "USER_NOT_FOUND"));
            }

            String userCode = userOpt.get().getUserCode();

            logger.info("예약 생성 요청 - 사용자: {}, 시설: {}", userCode, requestDto.getFacilityId());

            MyReservationDto reservation = reservationService.createReservation(requestDto, userCode, userId);

            return ResponseEntity.ok(new ApiResponse<>(true, "예약이 생성되었습니다. 관리자 승인을 기다려주세요.", reservation));

        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("예약 생성 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "RESERVATION_FAILED"));

        } catch (Exception e) {
            logger.error("예약 생성 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "예약 생성 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 예약 취소
     */
    @RateLimit(timeWindow = 30, maxRequests = 2, message = "취소 요청이 너무 빈번합니다.")
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            HttpServletRequest request,
            @Valid @RequestBody ReservationCancelRequestDto requestDto) {

        try {
            // JWT 검증 및 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            String userCode = getUserCodeFromToken(token);

            logger.info("예약 취소 요청 - 예약ID: {}, 사용자: {}", requestDto.getReservationId(), userCode);

            reservationService.cancelReservation(requestDto.getReservationId(), userCode);

            return ResponseEntity.ok(new ApiResponse<>(true, "예약이 취소되었습니다.", null));

        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("예약 취소 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "CANCEL_FAILED"));

        } catch (Exception e) {
            logger.error("예약 취소 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "예약 취소 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 내 예약 목록 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 10, message = "조회가 너무 빈번합니다.")
    @PostMapping("/my")
    public ResponseEntity<ApiResponse<MyReservationListDto>> getMyReservations(
            HttpServletRequest request) {

        try {
            // JWT 검증 및 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            String userCode = getUserCodeFromToken(token);

            logger.info("내 예약 조회 - 사용자: {}", userCode);

            MyReservationListDto reservations = reservationService.getMyReservations(userCode);

            return ResponseEntity.ok(new ApiResponse<>(true, "내 예약 목록을 조회했습니다.", reservations));

        } catch (Exception e) {
            logger.error("예약 목록 조회 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "목록 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰에서 사용자 코드 추출
     */
    private String getUserCodeFromToken(String token) {
        Integer userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            throw new RuntimeException("토큰에서 사용자 정보를 찾을 수 없습니다.");
        }

        Optional<UserTbl> userOpt = userTblRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("존재하지 않는 사용자입니다.");
        }

        return userOpt.get().getUserCode();
    }
}
```

### 7.3 `AdminFacilityReservationController.java`

```java
package BlueCrab.com.example.controller;

import BlueCrab.com.example.annotation.RateLimit;
import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.repository.AdminTblRepository;
import BlueCrab.com.example.service.AdminFacilityReservationService;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * 시설물 예약 관리자 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/facility-reservations")
public class AdminFacilityReservationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminFacilityReservationController.class);

    @Autowired
    private AdminFacilityReservationService adminReservationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminTblRepository adminTblRepository;

    /**
     * 승인 대기 목록 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 20, message = "조회가 너무 빈번합니다.")
    @PostMapping("/pending")
    public ResponseEntity<ApiResponse<List<MyReservationDto>>> getPendingReservations(
            HttpServletRequest request) {

        try {
            // 관리자 인증 확인
            if (!isAdmin(request)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse<>(false, "관리자 권한이 필요합니다.", null, "FORBIDDEN"));
            }

            logger.info("승인 대기 목록 조회");

            List<MyReservationDto> pending = adminReservationService.getPendingReservations();

            return ResponseEntity.ok(new ApiResponse<>(true, "승인 대기 목록을 조회했습니다.", pending));

        } catch (Exception e) {
            logger.error("승인 대기 목록 조회 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "목록 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 전체 예약 목록 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 20, message = "조회가 너무 빈번합니다.")
    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<MyReservationDto>>> getAllReservations(
            HttpServletRequest request) {

        try {
            // 관리자 인증 확인
            if (!isAdmin(request)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse<>(false, "관리자 권한이 필요합니다.", null, "FORBIDDEN"));
            }

            logger.info("전체 예약 목록 조회");

            List<MyReservationDto> allReservations = adminReservationService.getAllReservations();

            return ResponseEntity.ok(new ApiResponse<>(true, "전체 예약 목록을 조회했습니다.", allReservations));

        } catch (Exception e) {
            logger.error("전체 예약 목록 조회 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "목록 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 대시보드 통계 조회
     */
    @RateLimit(timeWindow = 5, maxRequests = 30, message = "조회가 너무 빈번합니다.")
    @PostMapping("/stats")
    public ResponseEntity<ApiResponse<AdminReservationStatsDto>> getDashboardStats(
            HttpServletRequest request) {

        try {
            // 관리자 인증 확인
            if (!isAdmin(request)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse<>(false, "관리자 권한이 필요합니다.", null, "FORBIDDEN"));
            }

            logger.info("대시보드 통계 조회");

            AdminReservationStatsDto stats = adminReservationService.getDashboardStats();

            return ResponseEntity.ok(new ApiResponse<>(true, "통계 정보를 조회했습니다.", stats));

        } catch (Exception e) {
            logger.error("통계 조회 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "통계 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 예약 승인
     */
    @RateLimit(timeWindow = 30, maxRequests = 10, message = "승인 요청이 너무 빈번합니다.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveReservation(
            HttpServletRequest request,
            @PathVariable Integer id,
            @Valid @RequestBody AdminReservationDecisionRequestDto requestDto) {

        try {
            // 관리자 인증 및 정보 추출
            AdminInfo adminInfo = getAdminInfo(request);
            if (adminInfo == null) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse<>(false, "관리자 권한이 필요합니다.", null, "FORBIDDEN"));
            }

            logger.info("예약 승인 요청 - 예약ID: {}, 관리자: {}", id, adminInfo.adminCode);

            adminReservationService.approveReservation(
                    id, adminInfo.adminId, adminInfo.adminCode, requestDto.getNote()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "예약이 승인되었습니다.", null));

        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("예약 승인 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "APPROVE_FAILED"));

        } catch (Exception e) {
            logger.error("예약 승인 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "예약 승인 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 예약 반려
     */
    @RateLimit(timeWindow = 30, maxRequests = 10, message = "반려 요청이 너무 빈번합니다.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReservation(
            HttpServletRequest request,
            @PathVariable Integer id,
            @Valid @RequestBody AdminReservationDecisionRequestDto requestDto) {

        try {
            // 관리자 인증 및 정보 추출
            AdminInfo adminInfo = getAdminInfo(request);
            if (adminInfo == null) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse<>(false, "관리자 권한이 필요합니다.", null, "FORBIDDEN"));
            }

            logger.info("예약 반려 요청 - 예약ID: {}, 관리자: {}", id, adminInfo.adminCode);

            adminReservationService.rejectReservation(
                    id, adminInfo.adminId, adminInfo.adminCode, requestDto.getRejectionReason()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "예약이 반려되었습니다.", null));

        } catch (IllegalArgumentException e) {
            logger.warn("예약 반려 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "REJECT_FAILED"));

        } catch (Exception e) {
            logger.error("예약 반려 중 오류", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "예약 반려 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 관리자 인증 확인
     */
    private boolean isAdmin(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return false;
        }

        // TODO: 관리자 토큰 검증 로직 (기존 AdminController 패턴 참고)
        // 현재는 간단히 AdminTbl 존재 여부로 확인
        Integer userId = jwtUtil.extractUserId(token);
        return userId != null && adminTblRepository.existsById(userId);
    }

    /**
     * 관리자 정보 추출
     */
    private AdminInfo getAdminInfo(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }

        Integer adminId = jwtUtil.extractUserId(token);
        Optional<AdminTbl> adminOpt = adminTblRepository.findById(adminId);

        if (!adminOpt.isPresent()) {
            return null;
        }

        AdminTbl admin = adminOpt.get();
        return new AdminInfo(admin.getAdminIdx(), admin.getAdminEmail());
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 관리자 정보 내부 클래스
     */
    private static class AdminInfo {
        Integer adminId;
        String adminCode;

        AdminInfo(Integer adminId, String adminCode) {
            this.adminId = adminId;
            this.adminCode = adminCode;
        }
    }
}
```

---

## 8. Config & 정책

### 8.1 `FacilityConfig.java`

```java
package BlueCrab.com.example.config;

import BlueCrab.com.example.entity.Facility;
import BlueCrab.com.example.repository.FacilityRepository;
import BlueCrab.com.example.service.FacilityReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 시설물 예약 시스템 설정
 */
@Configuration
public class FacilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(FacilityConfig.class);

    /**
     * 초기 시설물 데이터 생성
     */
    @Bean
    public CommandLineRunner initFacilityData(FacilityRepository facilityRepository,
                                                FacilityReservationService reservationService) {
        return args -> {
            // 이미 데이터가 있으면 스킵
            if (facilityRepository.count() > 0) {
                logger.info("시설물 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
                return;
            }

            logger.info("시설물 초기 데이터 생성 시작");

            List<Facility> facilities = Arrays.asList(
                    // 스터디룸
                    new Facility("스터디룸 A", "STUDY", "소형 스터디룸 (조용한 학습 공간)",
                                 6, "도서관 2층", "화이트보드,마커,USB 콘센트"),
                    new Facility("스터디룸 B", "STUDY", "중형 스터디룸 (그룹 스터디용)",
                                 10, "도서관 2층", "화이트보드,마커,USB 콘센트,TV"),
                    new Facility("스터디룸 C", "STUDY", "대형 스터디룸 (프로젝트 팀용)",
                                 15, "도서관 3층", "화이트보드,마커,USB 콘센트,TV,빔프로젝터"),

                    // 세미나실
                    new Facility("세미나실 A", "SEMINAR", "소형 세미나실",
                                 20, "본관 3층", "빔프로젝터,마이크,화이트보드"),
                    new Facility("세미나실 B", "SEMINAR", "중형 세미나실",
                                 40, "본관 3층", "빔프로젝터,마이크,화이트보드,음향시스템"),
                    new Facility("세미나실 C", "SEMINAR", "대형 세미나실",
                                 80, "본관 4층", "빔프로젝터,마이크,화이트보드,음향시스템,무선인터넷"),

                    // 강당
                    new Facility("대강당", "AUDITORIUM", "대형 행사용 강당",
                                 200, "본관 1층", "무대,음향시스템,조명,빔프로젝터,마이크"),

                    // 체육시설
                    new Facility("체육관", "GYM", "실내 체육관",
                                 50, "체육관 건물", "농구대,배구네트,탁구대"),

                    // 장비
                    new Facility("노트북 A", "EQUIPMENT", "고성능 노트북 (영상편집용)",
                                 null, "교무실", "Adobe Creative Cloud 설치"),
                    new Facility("카메라 세트", "EQUIPMENT", "전문가용 DSLR 카메라",
                                 null, "교무실", "렌즈,삼각대,조명 포함")
            );

            facilityRepository.saveAll(facilities);

            logger.info("시설물 초기 데이터 {}건 생성 완료", facilities.size());
        };
    }
}
```

---

## 9. 스케줄러

### 9.1 `FacilityReservationScheduler.java`

```java
package BlueCrab.com.example.scheduler;

import BlueCrab.com.example.service.FacilityReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 시설물 예약 스케줄러
 */
@Component
public class FacilityReservationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FacilityReservationScheduler.class);

    @Autowired
    private FacilityReservationService reservationService;

    /**
     * 만료된 예약 자동 완료 처리 (10분마다)
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void completeExpiredReservations() {
        logger.info("만료된 예약 자동 완료 스케줄러 실행");
        try {
            reservationService.completeExpiredReservations();
        } catch (Exception e) {
            logger.error("만료된 예약 처리 중 오류", e);
        }
    }

    /**
     * 일일 통계 집계 (매일 새벽 2시)
     * TODO: 추후 통계 테이블 구현 시 활성화
     */
    // @Scheduled(cron = "0 0 2 * * *")
    public void aggregateDailyStats() {
        logger.info("일일 통계 집계 스케줄러 실행");
        // TODO: 통계 집계 로직 구현
    }
}
```

---

## 10. 구현 순서

### ✅ Phase 1: 기반 구축 (1일차)
1. ✅ Entity 클래스 작성 (Facility, FacilityReservation, FacilityReservationLog)
2. ✅ Repository 인터페이스 작성 (3개)
3. ✅ DTO 클래스 작성 (Request 5개, Response 9개)
4. ✅ DB 테이블 생성 (DDL 실행)

### ✅ Phase 2: 핵심 로직 (2일차)
5. ✅ FacilityService 구현
6. ✅ FacilityReservationService 구현
7. ✅ AdminFacilityReservationService 구현
8. ✅ 단위 테스트 작성

### ✅ Phase 3: API 구현 (3일차)
9. ✅ FacilityController 구현
10. ✅ FacilityReservationController 구현
11. ✅ AdminFacilityReservationController 구현
12. ✅ FacilityConfig 초기 데이터 설정

### ✅ Phase 4: 부가 기능 (4일차)
13. ✅ FacilityReservationScheduler 구현
14. ✅ 예외 처리 강화
15. ✅ 로깅 추가
16. ✅ Rate Limit 설정

### ✅ Phase 5: 테스트 & 문서화 (5일차)
17. ⬜ API 통합 테스트 (Postman/cURL)
18. ⬜ 프론트엔드 연동 테스트
19. ⬜ API 문서 작성 (claudedocs/)
20. ⬜ 배포 및 최종 검증

---

## 📊 API 엔드포인트 요약

### 사용자 API
```
POST /api/facilities/list                      # 시설물 목록
POST /api/facilities/{id}/detail               # 시설물 상세
POST /api/facilities/{id}/availability         # 예약 가능 시간대

POST /api/facility-reservations/reserve        # 예약 생성
POST /api/facility-reservations/cancel         # 예약 취소
POST /api/facility-reservations/my             # 내 예약
```

### 관리자 API
```
POST /api/admin/facility-reservations/pending  # 승인 대기 목록
POST /api/admin/facility-reservations/list     # 전체 예약 목록
POST /api/admin/facility-reservations/stats    # 대시보드 통계
POST /api/admin/facility-reservations/{id}/approve  # 예약 승인
POST /api/admin/facility-reservations/{id}/reject   # 예약 반려
```

---

## 🎯 핵심 기능 정리

### ✅ 구현 완료
- 시설물 CRUD (조회 위주)
- 시간대 기반 예약 시스템
- 관리자 승인 워크플로우 (PENDING → APPROVED/REJECTED)
- 감사 로그 (모든 상태 전이 추적)
- 사용자별 예약 제한 (최대 5개)
- 예약 가능 시간대 조회 (1시간 단위 슬롯)
- 자동 만료 처리 (스케줄러)
- 관리자 대시보드 통계

### 🔜 향후 확장 (선택)
- FCM 푸시 알림 (승인/반려 알림)
- 예약 대기열 (Waitlist)
- 반복 예약 (매주 같은 시간)
- Redis 캐시 (통계 최적화)
- 관리자 시설물 CRUD (현재는 초기 데이터만)

---

**작성일**: 2025-10-06
**버전**: 2.0 (Final)
**기반**: 열람실(ReadingRoom) + GPT-5 Blueprint + 완전한 코드 구현
**상태**: 즉시 구현 가능 ✅
