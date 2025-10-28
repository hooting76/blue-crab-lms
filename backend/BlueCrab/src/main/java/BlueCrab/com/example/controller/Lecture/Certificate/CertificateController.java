package BlueCrab.com.example.controller.Lecture.Certificate;

import BlueCrab.com.example.dto.Lecture.Certificate.TranscriptResponse;
import BlueCrab.com.example.dto.Lecture.Certificate.GradeRecord;
import BlueCrab.com.example.service.Lecture.Certificate.CertificateService;
import BlueCrab.com.example.util.JwtUtil;
import BlueCrab.com.example.exception.Lecture.Certificate.StudentNotFoundException;
import BlueCrab.com.example.exception.Lecture.Certificate.LectureNotFoundException;
import BlueCrab.com.example.exception.Lecture.Certificate.InvalidGradeDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 성적확인서 발급 REST API 컨트롤러
 * 학생의 성적확인서 조회 및 발급 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
public class CertificateController {
    
    private final CertificateService certificateService;
    private final JwtUtil jwtUtil;
    
    /**
     * 학생 본인의 성적확인서 조회
     * 
     * POST /api/certificate/transcript
     * 
     * 인증: JWT 토큰 필수 (학생만 접근 가능)
     * 
     * Request Body:
     * {
     *   "action": "get-my-transcript"
     * }
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "성적확인서 조회 성공",
     *   "data": {
     *     "student": {
     *       "studentIdx": 33,
     *       "studentCode": "202500106114",
     *       "name": "김민준",
     *       "grade": 1,
     *       "admissionYear": 2025
     *     },
     *     "gradeRecords": [
     *       {
     *         "lecSerial": "CS284",
     *         "lecTitle": "컴퓨터과학개론",
     *         "credits": 3,
     *         "professorName": "김교수",
     *         "year": 2025,
     *         "semester": 1,
     *         "percentage": 92.50,
     *         "letterGrade": "A0",
     *         "gradeStatus": "COMPLETED"
     *       }
     *     ],
     *     "overallSummary": {
     *       "totalEarnedCredits": 18,
     *       "cumulativeGpa": 3.85,
     *       "averagePercentage": 87.50
     *     },
     *     "issuedAt": "2025-10-28T15:30:00",
     *     "certificateNumber": "TR-202500106114-20251028153000"
     *   }
     * }
     */
    @PostMapping("/transcript")
    public ResponseEntity<Map<String, Object>> getMyTranscript(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            // JWT에서 학생 정보 추출 (강화된 null 체크)
            String token = extractToken(request);
            if (token == null || token.trim().isEmpty()) {
                log.warn("JWT 토큰이 없음 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("인증 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT 토큰 유효성 검증
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT에서 학생 IDX 추출
            Integer studentIdx = jwtUtil.extractUserId(token);
            if (studentIdx == null) {
                log.warn("JWT에서 사용자 ID 추출 실패");
                return createErrorResponse("토큰에서 사용자 정보를 추출할 수 없습니다.", HttpStatus.UNAUTHORIZED);
            }
            
            log.info("성적확인서 조회 요청 - 학생 IDX: {}", studentIdx);
            
            // 성적확인서 생성
            TranscriptResponse transcript = certificateService.generateTranscript(studentIdx);
            
            // Null 체크
            if (transcript == null) {
                log.error("성적확인서 생성 실패 - 학생 IDX: {}", studentIdx);
                return createErrorResponse("성적확인서 생성에 실패했습니다.", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            return createSuccessResponse("성적확인서 조회 성공", transcript);
            
        } catch (StudentNotFoundException e) {
            log.warn("학생 없음: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (LectureNotFoundException e) {
            log.error("강의 정보 없음: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InvalidGradeDataException e) {
            log.error("성적 데이터 오류: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("성적확인서 조회 실패", e);
            return createErrorResponse("성적확인서 조회 중 오류가 발생했습니다: " + e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 특정 학생의 성적확인서 조회 (교수/관리자용)
     * 
     * POST /api/certificate/transcript/student
     * 
     * 인증: JWT 토큰 필수 (교수 또는 관리자만 접근 가능)
     * 
     * Request Body:
     * {
     *   "action": "get-student-transcript",
     *   "studentIdx": 33
     * }
     * 
     * @param requestBody 요청 본문 (studentIdx 포함)
     */
    @PostMapping("/transcript/student")
    public ResponseEntity<Map<String, Object>> getStudentTranscript(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            // JWT에서 사용자 정보 추출 (강화된 null 체크)
            String token = extractToken(request);
            if (token == null || token.trim().isEmpty()) {
                log.warn("JWT 토큰이 없음 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("인증 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT 토큰 유효성 검증
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT에서 요청자 IDX 추출
            Integer requesterIdx = jwtUtil.extractUserId(token);
            if (requesterIdx == null) {
                log.warn("JWT에서 사용자 ID 추출 실패");
                return createErrorResponse("토큰에서 사용자 정보를 추출할 수 없습니다.", HttpStatus.UNAUTHORIZED);
            }
            
            log.info("성적확인서 조회 요청 (교수/관리자) - 요청자 IDX: {}", requesterIdx);
            
            // studentIdx 추출 (안전한 변환)
            Integer studentIdx = extractStudentIdx(requestBody);
            if (studentIdx == null) {
                log.warn("studentIdx 누락 또는 잘못된 형식");
                return createErrorResponse("학생 IDX가 필요합니다.", HttpStatus.BAD_REQUEST);
            }
            
            log.info("성적확인서 조회 요청 - 요청자: {}, 대상 학생: {}", requesterIdx, studentIdx);
            
            // 성적확인서 생성
            TranscriptResponse transcript = certificateService.generateTranscript(studentIdx);
            
            // Null 체크
            if (transcript == null) {
                log.error("성적확인서 생성 실패 - 학생 IDX: {}", studentIdx);
                return createErrorResponse("성적확인서 생성에 실패했습니다.", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            return createSuccessResponse("성적확인서 조회 성공", transcript);
            
        } catch (StudentNotFoundException e) {
            log.warn("학생 없음: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (LectureNotFoundException e) {
            log.error("강의 정보 없음: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InvalidGradeDataException e) {
            log.error("성적 데이터 오류: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("성적확인서 조회 실패", e);
            return createErrorResponse("성적확인서 조회 중 오류가 발생했습니다: " + e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 성적확인서 PDF 다운로드 (선택 기능)
     * 
     * POST /api/certificate/transcript/download
     * 
     * Request Body:
     * {
     *   "action": "download-pdf"
     * }
     * 
     * TODO: PDF 생성 로직 구현 필요
     */
    @PostMapping("/transcript/download")
    public ResponseEntity<Map<String, Object>> downloadTranscriptPdf(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            // JWT에서 학생 정보 추출 (강화된 null 체크)
            String token = extractToken(request);
            if (token == null || token.trim().isEmpty()) {
                log.warn("JWT 토큰이 없음 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("인증 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT 토큰 유효성 검증
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT에서 학생 IDX 추출
            Integer studentIdx = jwtUtil.extractUserId(token);
            if (studentIdx == null) {
                log.warn("JWT에서 사용자 ID 추출 실패");
                return createErrorResponse("토큰에서 사용자 정보를 추출할 수 없습니다.", HttpStatus.UNAUTHORIZED);
            }
            
            log.info("PDF 다운로드 요청 - 학생 IDX: {}", studentIdx);
            
            // TODO: PDF 생성 및 다운로드 로직 구현
            return createErrorResponse("PDF 다운로드 기능은 아직 구현되지 않았습니다.", 
                HttpStatus.NOT_IMPLEMENTED);
            
        } catch (Exception e) {
            log.error("PDF 다운로드 실패", e);
            return createErrorResponse("PDF 다운로드 중 오류가 발생했습니다: " + e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 학기별 성적 조회
     * 
     * POST /api/certificate/semester
     * 
     * Request Body:
     * {
     *   "action": "get-semester-grades",
     *   "year": 2025,
     *   "semester": 1
     * }
     * 
     * @param requestBody 요청 본문 (year, semester 포함)
     */
    @PostMapping("/semester")
    public ResponseEntity<Map<String, Object>> getSemesterGrades(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            // JWT에서 학생 정보 추출 (강화된 null 체크)
            String token = extractToken(request);
            if (token == null || token.trim().isEmpty()) {
                log.warn("JWT 토큰이 없음 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("인증 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT 토큰 유효성 검증
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰 - IP: {}", request.getRemoteAddr());
                return createErrorResponse("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // JWT에서 학생 IDX 추출
            Integer studentIdx = jwtUtil.extractUserId(token);
            if (studentIdx == null) {
                log.warn("JWT에서 사용자 ID 추출 실패");
                return createErrorResponse("토큰에서 사용자 정보를 추출할 수 없습니다.", HttpStatus.UNAUTHORIZED);
            }
            
            // year, semester 추출 (final 키워드 추가)
            final Integer year;
            final Integer semester;
            
            // year 파싱
            if (requestBody.containsKey("year")) {
                Object yearObj = requestBody.get("year");
                if (yearObj instanceof Integer) {
                    year = (Integer) yearObj;
                } else if (yearObj instanceof String) {
                    year = Integer.parseInt((String) yearObj);
                } else {
                    year = null;
                }
            } else {
                year = null;
            }
            
            // semester 파싱
            if (requestBody.containsKey("semester")) {
                Object semesterObj = requestBody.get("semester");
                if (semesterObj instanceof Integer) {
                    semester = (Integer) semesterObj;
                } else if (semesterObj instanceof String) {
                    semester = Integer.parseInt((String) semesterObj);
                } else {
                    semester = null;
                }
            } else {
                semester = null;
            }
            
            if (year == null || semester == null) {
                return createErrorResponse("year와 semester가 필요합니다.", HttpStatus.BAD_REQUEST);
            }
            
            log.info("학기별 성적 조회 - 학생 IDX: {}, 년도: {}, 학기: {}", studentIdx, year, semester);
            
            // 전체 성적확인서 조회
            TranscriptResponse transcript = certificateService.generateTranscript(studentIdx);
            
            // 해당 학기 성적만 필터링
            String semesterKey = year + "-" + semester;
            TranscriptResponse.SemesterSummary semesterSummary = 
                transcript.getSemesterSummaries().get(semesterKey);
            
            if (semesterSummary == null) {
                return createErrorResponse("해당 학기의 성적 정보가 없습니다.", HttpStatus.NOT_FOUND);
            }
            
            // 해당 학기 성적 레코드 필터링 (var → 명시적 타입 선언)
            List<GradeRecord> semesterRecords = transcript.getGradeRecords().stream()
                .filter(r -> r.getYear().equals(year) && r.getSemester().equals(semester))
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("semester", semesterSummary);
            result.put("gradeRecords", semesterRecords);
            
            log.info("학기별 성적 조회 성공 - 학생 IDX: {}, 성적 수: {}", 
                studentIdx, semesterRecords.size());
            
            return createSuccessResponse("학기별 성적 조회 성공", result);
            
        } catch (Exception e) {
            log.error("학기별 성적 조회 실패", e);
            return createErrorResponse("학기별 성적 조회 중 오류가 발생했습니다: " + e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * requestBody에서 studentIdx 안전하게 추출
     * TODO: JWT 구현 후 이 메서드는 제거 예정
     */
    private Integer extractStudentIdx(Map<String, Object> requestBody) {
        if (requestBody == null) {
            log.warn("requestBody가 null입니다.");
            return null;
        }
        
        try {
            Object idxObj = requestBody.get("studentIdx");
            if (idxObj == null) {
                log.warn("studentIdx가 requestBody에 없습니다.");
                return null;
            }
            
            if (idxObj instanceof Integer) {
                return (Integer) idxObj;
            } else if (idxObj instanceof String) {
                return Integer.parseInt((String) idxObj);
            } else if (idxObj instanceof Number) {
                return ((Number) idxObj).intValue();
            } else {
                log.warn("studentIdx 타입이 지원되지 않습니다: {}", idxObj.getClass().getName());
                return null;
            }
        } catch (NumberFormatException e) {
            log.error("studentIdx 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 학기별 GPA 계산
     */
    private BigDecimal calculateSemesterGpa(List<GradeRecord> semesterRecords) {
        if (semesterRecords == null || semesterRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalWeightedGpa = BigDecimal.ZERO;
        int totalCredits = 0;
        
        for (GradeRecord record : semesterRecords) {
            if (record == null || record.getCredits() == null || record.getLetterGrade() == null) {
                continue;
            }
            
            // GPA 평점 변환 (letterGrade → 평점)
            BigDecimal gradePoint = convertLetterGradeToPoint(record.getLetterGrade());
            if (gradePoint == null) {
                continue;
            }
            
            totalWeightedGpa = totalWeightedGpa.add(
                gradePoint.multiply(BigDecimal.valueOf(record.getCredits()))
            );
            totalCredits += record.getCredits();
        }
        
        if (totalCredits == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalWeightedGpa.divide(
            BigDecimal.valueOf(totalCredits), 
            2, 
            java.math.RoundingMode.HALF_UP
        );
    }
    
    /**
     * 문자 등급을 평점으로 변환
     */
    private BigDecimal convertLetterGradeToPoint(String letterGrade) {
        if (letterGrade == null) {
            return null;
        }
        
        switch (letterGrade.trim().toUpperCase()) {
            case "A+": return new BigDecimal("4.5");
            case "A0": case "A": return new BigDecimal("4.0");
            case "B+": return new BigDecimal("3.5");
            case "B0": case "B": return new BigDecimal("3.0");
            case "C+": return new BigDecimal("2.5");
            case "C0": case "C": return new BigDecimal("2.0");
            case "D+": return new BigDecimal("1.5");
            case "D0": case "D": return new BigDecimal("1.0");
            case "F": return new BigDecimal("0.0");
            default:
                log.warn("알 수 없는 학점 등급: {}", letterGrade);
                return null;
        }
    }
    
    /**
     * 성공 응답 생성
     */
    private ResponseEntity<Map<String, Object>> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 에러 응답 생성
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        return ResponseEntity.status(status).body(response);
    }
}
