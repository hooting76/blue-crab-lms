// 작성자: 성태준
// 과제 관리 서비스

package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.AssignmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.AssignmentExtendedTblRepository;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/* 과제 관리 비즈니스 로직 서비스
 * 과제 생성, 제출, 채점 등의 비즈니스 로직 처리
 *
 * 주요 기능:
 * - 과제 등록/수정/삭제 (CRUD)
 * - 과제 조회 및 검색
 * - 과제 제출 관리
 * - JSON 데이터 파싱 (assignmentData)
 * - 과제 통계
 *
 * 트랜잭션 관리:
 * - 조회 작업: @Transactional(readOnly = true)
 * - 변경 작업: @Transactional
 *
 * JSON 데이터 구조 (assignmentData):
 * {
 *   "assignment": {
 *     "title": "과제 제목",
 *     "description": "과제 설명",
 *     "dueDate": "2025-03-20T23:59:59",
 *     "maxScore": 100
 *   },
 *   "submissions": [
 *     {
 *       "studentIdx": 123,
 *       "submittedAt": "2025-03-19T14:30:00",
 *       "fileUrl": "https://...",
 *       "score": 85,
 *       "feedback": "잘했습니다",
 *       "gradedAt": "2025-03-21T10:00:00"
 *     }
 *   ]
 * }
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    @Autowired
    private AssignmentExtendedTblRepository assignmentRepository;

    @Autowired
    private LecTblRepository lecTblRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 과제 조회 메서드 ==========

    /* 과제 IDX로 단건 조회 */
    public Optional<AssignmentExtendedTbl> getAssignmentById(Integer assignmentIdx) {
        return assignmentRepository.findById(assignmentIdx);
    }

    /* 강의별 과제 목록 조회 (최신순) */
    public List<AssignmentExtendedTbl> getAssignmentsByLecture(Integer lecIdx) {
        return assignmentRepository.findByLecIdxOrderByAssignmentIdxDesc(lecIdx);
    }

    /* 강의별 과제 목록 조회 (페이징) */
    public Page<AssignmentExtendedTbl> getAssignmentsByLecturePaged(Integer lecIdx, Pageable pageable) {
        return assignmentRepository.findByLecIdx(lecIdx, pageable);
    }

    /* 강의별 과제 목록 조회 (강의 정보 포함) */
    public List<AssignmentExtendedTbl> getAssignmentsWithLecture(Integer lecIdx) {
        return assignmentRepository.findAssignmentsWithLecture(lecIdx);
    }

    /* 여러 강의의 과제 목록 일괄 조회 */
    public List<AssignmentExtendedTbl> getAssignmentsByLectures(List<Integer> lecIdxList) {
        return assignmentRepository.findAllByLecIdxIn(lecIdxList);
    }

    /* 전체 과제 목록 조회 (페이징) */
    public Page<AssignmentExtendedTbl> getAllAssignments(Pageable pageable) {
        return assignmentRepository.findAll(pageable);
    }

    // ========== 과제 등록/수정/삭제 메서드 ==========

    /* 과제 등록 */
    @Transactional
    public AssignmentExtendedTbl createAssignment(Integer lecIdx, String title, String description, String dueDate, Integer maxScore) {
        // 강의 존재 여부 확인
        lecTblRepository.findById(lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다: " + lecIdx));

        AssignmentExtendedTbl assignment = new AssignmentExtendedTbl();
        assignment.setLecIdx(lecIdx);

        // 초기 JSON 데이터 생성
        String initialData = createInitialAssignmentData(title, description, dueDate, maxScore);
        assignment.setAssignmentData(initialData);

        return assignmentRepository.save(assignment);
    }

    /* 과제 수정 */
    @Transactional
    @SuppressWarnings("unchecked")
    public AssignmentExtendedTbl updateAssignment(Integer assignmentIdx, String title, String description, String dueDate, Integer maxScore) {
        AssignmentExtendedTbl assignment = assignmentRepository.findById(assignmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다: " + assignmentIdx));

        try {
            Map<String, Object> data = parseAssignmentData(assignment.getAssignmentData());
            Map<String, Object> assignmentInfo = (Map<String, Object>) data.getOrDefault("assignment", Map.of());
            
            if (title != null) assignmentInfo.put("title", title);
            if (description != null) assignmentInfo.put("description", description);
            if (dueDate != null) assignmentInfo.put("dueDate", dueDate);
            if (maxScore != null) assignmentInfo.put("maxScore", maxScore);
            
            data.put("assignment", assignmentInfo);
            String jsonData = objectMapper.writeValueAsString(data);
            assignment.setAssignmentData(jsonData);
            
            return assignmentRepository.save(assignment);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("과제 데이터 변환 실패", e);
        }
    }

    /* 과제 삭제 */
    @Transactional
    public void deleteAssignment(Integer assignmentIdx) {
        if (!assignmentRepository.existsById(assignmentIdx)) {
            throw new IllegalArgumentException("존재하지 않는 과제입니다: " + assignmentIdx);
        }
        assignmentRepository.deleteById(assignmentIdx);
    }

    // ========== 과제 제출 관리 메서드 ==========

    /* 과제 제출 추가 */
    @Transactional
    @SuppressWarnings("unchecked")
    public AssignmentExtendedTbl submitAssignment(Integer assignmentIdx, Integer studentIdx, String fileUrl) {
        AssignmentExtendedTbl assignment = assignmentRepository.findById(assignmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다: " + assignmentIdx));

        try {
            Map<String, Object> data = parseAssignmentData(assignment.getAssignmentData());
            List<Map<String, Object>> submissions = (List<Map<String, Object>>) data.getOrDefault("submissions", List.of());
            
            Map<String, Object> newSubmission = Map.of(
                "studentIdx", studentIdx,
                "submittedAt", getCurrentDateTime(),
                "fileUrl", fileUrl,
                "score", 0,
                "feedback", "",
                "gradedAt", ""
            );
            
            submissions.add(newSubmission);
            data.put("submissions", submissions);
            
            String jsonData = objectMapper.writeValueAsString(data);
            assignment.setAssignmentData(jsonData);
            
            return assignmentRepository.save(assignment);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("과제 제출 데이터 변환 실패", e);
        }
    }

    /* 과제 채점 */
    @Transactional
    @SuppressWarnings("unchecked")
    public AssignmentExtendedTbl gradeAssignment(Integer assignmentIdx, Integer studentIdx, Integer score, String feedback) {
        AssignmentExtendedTbl assignment = assignmentRepository.findById(assignmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다: " + assignmentIdx));

        try {
            Map<String, Object> data = parseAssignmentData(assignment.getAssignmentData());
            List<Map<String, Object>> submissions = (List<Map<String, Object>>) data.getOrDefault("submissions", List.of());
            
            // 해당 학생의 제출물 찾기
            for (Map<String, Object> submission : submissions) {
                if (submission.get("studentIdx").equals(studentIdx)) {
                    submission.put("score", score);
                    submission.put("feedback", feedback);
                    submission.put("gradedAt", getCurrentDateTime());
                    break;
                }
            }
            
            data.put("submissions", submissions);
            String jsonData = objectMapper.writeValueAsString(data);
            assignment.setAssignmentData(jsonData);
            
            return assignmentRepository.save(assignment);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("채점 데이터 변환 실패", e);
        }
    }

    // ========== JSON 데이터 파싱 메서드 ==========

    /* 초기 assignmentData JSON 생성 */
    private String createInitialAssignmentData(String title, String description, String dueDate, Integer maxScore) {
        try {
            Map<String, Object> data = Map.of(
                "assignment", Map.of(
                    "title", title,
                    "description", description,
                    "dueDate", dueDate,
                    "maxScore", maxScore
                ),
                "submissions", List.of()
            );
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /* assignmentData 전체 JSON 파싱 */
    public Map<String, Object> parseAssignmentData(String assignmentJson) {
        if (assignmentJson == null || assignmentJson.trim().isEmpty() || "{}".equals(assignmentJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(assignmentJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("assignmentData 파싱 실패", e);
        }
    }

    // ========== 통계 메서드 ==========

    /* 강의별 과제 수 조회 */
    public long countAssignmentsByLecture(Integer lecIdx) {
        return assignmentRepository.countByLecIdx(lecIdx);
    }

    /* 전체 과제 수 조회 */
    public long countAllAssignments() {
        return assignmentRepository.countAllAssignments();
    }

    // ========== 배치 삭제 메서드 ==========

    /* 특정 강의의 모든 과제 삭제 */
    @Transactional
    public void deleteAllAssignmentsByLecture(Integer lecIdx) {
        assignmentRepository.deleteByLecIdx(lecIdx);
    }

    /* 여러 강의의 과제 일괄 삭제 */
    @Transactional
    public void deleteAssignmentsByLectures(List<Integer> lecIdxList) {
        List<AssignmentExtendedTbl> assignments = assignmentRepository.findAllByLecIdxIn(lecIdxList);
        assignmentRepository.deleteAll(assignments);
    }

    // ========== 유틸리티 메서드 ==========

    /* 현재 날짜/시간을 문자열로 반환 (yyyy-MM-dd HH:mm:ss 형식) */
    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /* 과제 존재 여부 확인 */
    public boolean existsAssignment(Integer assignmentIdx) {
        return assignmentRepository.existsById(assignmentIdx);
    }
}
