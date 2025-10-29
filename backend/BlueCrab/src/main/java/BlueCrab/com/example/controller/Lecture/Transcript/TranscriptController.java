package BlueCrab.com.example.controller.Lecture.Transcript;

import BlueCrab.com.example.dto.Lecture.Transcript.TranscriptResponseDto;
import BlueCrab.com.example.service.Lecture.Transcript.TranscriptService;
import BlueCrab.com.example.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 성적확인서 API 컨트롤러
 * 
 * 엔드포인트: POST /api/transcript/view
 * 인증: JWT 토큰 필수
 * 권한: 본인 성적만 조회 가능
 */
@Slf4j
@RestController
@RequestMapping("/api/transcript")
@CrossOrigin(origins = {
    "https://dtmch.synology.me:56000",
    "https://bluecrab.chickenkiller.com",
    "https://bluecrab-front-test.chickenkiller.com"
})
@RequiredArgsConstructor
public class TranscriptController {
    
    private final TranscriptService transcriptService;
    private final JwtUtil jwtUtil;
    
    /**
     * 성적확인서 조회
     * 
     * 학생이 로그인하여 자신의 성적확인서를 조회합니다.
     * - 수료한 강의
     * - 중도 포기한 강의
     * - 낙제한 강의
     * - 이수학점, 성적(A~F), GPA(4.0 만점)
     * 
     * @param requestBody {studentIdx: Integer} - 요청 본문
     * @return 성적확인서
     */
    @PostMapping("/view")
    public ResponseEntity<Map<String, Object>> viewTranscript(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. JWT 토큰에서 USER_IDX 추출
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization 헤더 없음");
                response.put("status", "error");
                response.put("message", "인증이 필요합니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String token = authHeader.substring(7);
            Integer authenticatedUserIdx;
            try {
                authenticatedUserIdx = jwtUtil.extractUserId(token);
                if (authenticatedUserIdx == null) {
                    log.error("JWT 토큰에서 USER_IDX 추출 실패");
                    response.put("status", "error");
                    response.put("message", "잘못된 인증 정보입니다");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            } catch (Exception e) {
                log.error("JWT 토큰 파싱 실패", e);
                response.put("status", "error");
                response.put("message", "잘못된 인증 정보입니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 2. 요청 본문에서 studentIdx 추출
            Integer requestedStudentIdx = null;
            if (requestBody.containsKey("studentIdx")) {
                Object studentIdxObj = requestBody.get("studentIdx");
                if (studentIdxObj instanceof Integer) {
                    requestedStudentIdx = (Integer) studentIdxObj;
                } else if (studentIdxObj instanceof String) {
                    try {
                        requestedStudentIdx = Integer.parseInt((String) studentIdxObj);
                    } catch (NumberFormatException e) {
                        log.warn("studentIdx 파싱 실패: {}", studentIdxObj);
                    }
                }
            }
            
            // 3. 본인 확인 (요청한 학생 IDX == JWT의 USER_IDX)
            Integer targetStudentIdx = requestedStudentIdx != null 
                ? requestedStudentIdx 
                : authenticatedUserIdx;
            
            if (!targetStudentIdx.equals(authenticatedUserIdx)) {
                log.warn("권한 없음 - 요청한 학생: {}, 인증된 학생: {}", targetStudentIdx, authenticatedUserIdx);
                response.put("status", "error");
                response.put("message", "본인의 성적만 조회할 수 있습니다");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            log.info("성적확인서 조회 요청 - 학생 IDX: {}", targetStudentIdx);
            
            // 4. 성적확인서 생성
            TranscriptResponseDto transcript = transcriptService.generateTranscript(targetStudentIdx);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("성적확인서 조회 성공 - 학생 IDX: {}, 소요시간: {}ms", targetStudentIdx, duration);
            
            response.put("status", "success");
            response.put("message", "성적확인서 조회 성공");
            response.put("data", transcript);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("성적확인서 조회 실패 - 소요시간: {}ms, 에러: {}", duration, e.getMessage());
            
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("성적확인서 조회 중 서버 오류 - 소요시간: {}ms", duration, e);
            
            response.put("status", "error");
            response.put("message", "서버 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 헬스 체크 (테스트용)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Transcript API is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
