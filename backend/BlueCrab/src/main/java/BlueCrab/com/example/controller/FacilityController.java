package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.FacilityAvailabilityDto;
import BlueCrab.com.example.dto.FacilityDto;
import BlueCrab.com.example.enums.FacilityType;
import BlueCrab.com.example.service.FacilityReservationService;
import BlueCrab.com.example.service.FacilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        FacilityAvailabilityDto availability = reservationService.checkAvailability(
            facilityIdx, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success("가용성을 확인했습니다.", availability));
    }
}
