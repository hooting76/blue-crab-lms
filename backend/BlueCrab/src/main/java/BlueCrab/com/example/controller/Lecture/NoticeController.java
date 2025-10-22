package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.dto.Lecture.NoticeSaveRequest;
import BlueCrab.com.example.dto.Lecture.NoticeSaveResponse;
import BlueCrab.com.example.dto.Lecture.NoticeViewResponse;
import BlueCrab.com.example.entity.Lecture.CourseApplyNotice;
import BlueCrab.com.example.repository.Lecture.CourseApplyNoticeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 수강신청 안내문 API 컨트롤러
 * 
 * 주요 기능:
 * - 안내문 조회 (공개, 로그인 불필요)
 * - 안내문 저장 (관리자/교수만 가능)
 * 
 * 엔드포인트:
 * - POST /notice/course-apply/view (공개)
 * - POST /notice/course-apply/save (관리자/교수)
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-22
 */
@RestController
@RequestMapping("/notice/course-apply")
public class NoticeController {

    private static final Logger logger = LoggerFactory.getLogger(NoticeController.class);

    @Autowired
    private CourseApplyNoticeRepository noticeRepository;

    /**
     * 안내문 조회 (공개)
     * 
     * 로그인 불필요하며, 누구나 현재 안내문을 조회할 수 있습니다.
     * 최신 안내문 1개만 반환합니다.
     * 
     * @return 안내문 조회 응답
     */
    @PostMapping("/view")
    public ResponseEntity<?> viewNotice() {
        try {
            logger.info("안내문 조회 요청");

            CourseApplyNotice notice = noticeRepository
                    .findTopByOrderByUpdatedAtDesc()
                    .orElse(null);

            if (notice == null) {
                logger.warn("안내문이 존재하지 않습니다");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    NoticeViewResponse.builder()
                        .success(false)
                        .message("안내문이 없습니다.")
                        .build()
                );
            }

            logger.info("안내문 조회 성공: noticeIdx={}", notice.getNoticeIdx());
            return ResponseEntity.ok(
                NoticeViewResponse.builder()
                    .success(true)
                    .message(notice.getMessage())
                    .updatedAt(notice.getUpdatedAt())
                    .updatedBy(notice.getUpdatedBy())
                    .build()
            );

        } catch (Exception e) {
            logger.error("안내문 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "안내문 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 안내문 저장 (관리자/교수)
     * 
     * 관리자 또는 교수만 안내문을 작성하거나 수정할 수 있습니다.
     * 기존 안내문이 있으면 수정하고, 없으면 새로 생성합니다.
     * 
     * @param request 저장할 안내문 내용
     * @param authentication 인증 정보 (Spring Security에서 자동 주입)
     * @return 안내문 저장 응답
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveNotice(
            @RequestBody NoticeSaveRequest request,
            Authentication authentication) {
        try {
            // 인증 정보 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("인증되지 않은 사용자의 안내문 저장 시도");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(errorResponse);
            }

            String username = authentication.getName();
            logger.info("안내문 저장 요청: username={}", username);

            // 요청 데이터 검증
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                logger.warn("빈 메시지로 안내문 저장 시도");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "안내 메시지는 필수입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 기존 안내문 찾기 (있으면 수정, 없으면 신규)
            CourseApplyNotice notice = noticeRepository
                    .findTopByOrderByUpdatedAtDesc()
                    .orElse(new CourseApplyNotice());

            notice.setMessage(request.getMessage());
            notice.setUpdatedBy(username);

            CourseApplyNotice saved = noticeRepository.save(notice);

            logger.info("안내문 저장 성공: noticeIdx={}, username={}", 
                saved.getNoticeIdx(), username);

            return ResponseEntity.ok(
                NoticeSaveResponse.builder()
                    .success(true)
                    .message("안내문이 저장되었습니다.")
                    .data(NoticeSaveResponse.NoticeData.builder()
                        .noticeIdx(saved.getNoticeIdx())
                        .message(saved.getMessage())
                        .updatedAt(saved.getUpdatedAt())
                        .updatedBy(saved.getUpdatedBy())
                        .build())
                    .build()
            );

        } catch (Exception e) {
            logger.error("안내문 저장 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "안내문 저장 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}
