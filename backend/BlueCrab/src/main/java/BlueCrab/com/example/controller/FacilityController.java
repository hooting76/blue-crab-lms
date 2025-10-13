package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.DailyScheduleDto;
import BlueCrab.com.example.dto.FacilityAvailabilityDto;
import BlueCrab.com.example.dto.FacilityDto;
import BlueCrab.com.example.enums.FacilityType;
import BlueCrab.com.example.service.FacilityReservationService;
import BlueCrab.com.example.service.FacilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시설 조회 API 컨트롤러
 */
@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;
    private final FacilityReservationService reservationService;

    public FacilityController(FacilityService facilityService,
                              FacilityReservationService reservationService) {
        this.facilityService = facilityService;
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getAllFacilities() {
        List<FacilityDto> facilities = facilityService.getAllActiveFacilities();
        return ResponseEntity.ok(ApiResponse.success("시설 목록을 조회했습니다.", facilities));
    }

    @PostMapping("/type/{facilityType}")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getFacilitiesByType(
            @PathVariable String facilityType) {
        FacilityType type = FacilityType.fromString(facilityType);
        List<FacilityDto> facilities = facilityService.getFacilitiesByType(type);
        return ResponseEntity.ok(ApiResponse.success("시설 목록을 조회했습니다.", facilities));
    }

    @PostMapping("/{facilityIdx}")
    public ResponseEntity<ApiResponse<FacilityDto>> getFacilityById(
            @PathVariable Integer facilityIdx) {
        FacilityDto facility = facilityService.getFacilityById(facilityIdx);
        return ResponseEntity.ok(ApiResponse.success("시설 정보를 조회했습니다.", facility));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> searchFacilities(
            @RequestParam String keyword) {
        List<FacilityDto> facilities = facilityService.searchFacilities(keyword);
        return ResponseEntity.ok(ApiResponse.success("검색 결과를 조회했습니다.", facilities));
    }

    @PostMapping("/{facilityIdx}/availability")
    public ResponseEntity<ApiResponse<FacilityAvailabilityDto>> checkAvailability(
            @PathVariable Integer facilityIdx,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        // 기본 검증만 수행 (과거 날짜, 시작/종료 시간 순서)
        LocalDateTime now = LocalDateTime.now();
        
        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException("시작 시간은 현재 시간 이후여야 합니다.");
        }
        
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }
        
        // 시설별 상세 정책 검증은 Service에서 수행
        FacilityAvailabilityDto availability = reservationService.checkAvailability(
            facilityIdx, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success("가용성을 확인했습니다.", availability));
    }

    /**
     * 특정 날짜의 하루 전체 시간대별 예약 현황 조회
     * 프론트엔드에서 한 번의 API 호출로 하루 전체의 예약 상태를 확인 가능
     * 
     * @param facilityIdx 시설 ID
     * @param date 조회 날짜 (yyyy-MM-dd 형식)
     * @return 09:00~20:00까지 1시간 단위 시간대별 예약 상태
     */
    @PostMapping("/{facilityIdx}/daily-schedule")
    public ResponseEntity<ApiResponse<DailyScheduleDto>> getDailySchedule(
            @PathVariable Integer facilityIdx,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        DailyScheduleDto schedule = reservationService.getDailySchedule(facilityIdx, date);
        return ResponseEntity.ok(ApiResponse.success(
            "하루 일정을 조회했습니다.", schedule));
    }
}

