// 작성자: 성태준
// 수강신청 관리 서비스

package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/* 수강신청 관리 비즈니스 로직 서비스
 * 학생의 수강신청, 수강 취소, 성적 관리 등의 비즈니스 로직 처리
 *
 * 주요 기능:
 * - 수강신청 및 취소
 * - 수강 내역 조회
 * - 출석 및 성적 관리
 * - JSON 데이터 파싱 (enrollmentData)
 * - 수강신청 통계
 *
 * 트랜잭션 관리:
 * - 조회 작업: @Transactional(readOnly = true)
 * - 변경 작업: @Transactional
 *
 * JSON 데이터 구조 (enrollmentData):
 * {
 *   "enrollment": {"status": "ENROLLED", "enrollmentDate": "2025-03-01T09:00:00"},
 *   "attendance": [{"date": "2025-03-10", "status": "PRESENT"}],
 *   "grade": {"midterm": 85.5, "final": 92.0, "total": 88.9, "letterGrade": "A"}
 * }
 */
@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    @Autowired
    private EnrollmentExtendedTblRepository enrollmentRepository;

    @Autowired
    private LecTblRepository lecTblRepository;

    @Autowired
    private LectureService lectureService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 수강신청 조회 메서드 ==========

    /* 강의 코드로 강의 IDX 조회 (내부 변환용) */
    public Integer getLectureIdxBySerial(String lecSerial) {
        return lectureService.getLectureBySerial(lecSerial)
                .map(lecture -> lecture.getLecIdx())
                .orElse(null);
    }

    /* 수강신청 IDX로 단건 조회 */
    public Optional<EnrollmentExtendedTbl> getEnrollmentById(Integer enrollmentIdx) {
        return enrollmentRepository.findById(enrollmentIdx);
    }

    /* 전체 수강신청 목록 조회 (페이징) */
    public Page<EnrollmentExtendedTbl> getAllEnrollments(Pageable pageable) {
        return enrollmentRepository.findAll(pageable);
    }

    /* 학생 IDX로 수강 목록 조회 (강의 정보 포함) */
    public List<EnrollmentExtendedTbl> getEnrollmentsByStudent(Integer studentIdx) {
        return enrollmentRepository.findEnrolledLecturesByStudent(studentIdx);
    }

    /* 학생 IDX로 현재 수강중인 목록만 조회 */
    public List<EnrollmentExtendedTbl> getEnrolledByStudent(Integer studentIdx) {
        return enrollmentRepository.findEnrolledLecturesByStudent(studentIdx);
    }

    /* 학생 IDX로 수강 목록 조회 (페이징) */
    public Page<EnrollmentExtendedTbl> getEnrollmentsByStudentPaged(Integer studentIdx, Pageable pageable) {
        // JOIN FETCH로 lecture와 student를 함께 조회하여 Lazy Loading 방지
        return enrollmentRepository.findEnrollmentHistoryByStudent(studentIdx, pageable);
    }

    /* 강의 IDX로 수강생 목록 조회 (학생 정보 포함) */
    public List<EnrollmentExtendedTbl> getEnrollmentsByLecture(Integer lecIdx) {
        return enrollmentRepository.findStudentsByLecture(lecIdx);
    }

    /* 강의 IDX로 수강생 목록 조회 (페이징) */
    public Page<EnrollmentExtendedTbl> getEnrollmentsByLecturePaged(Integer lecIdx, Pageable pageable) {
        // JOIN FETCH로 lecture와 student를 함께 조회하여 Lazy Loading 방지
        return enrollmentRepository.findStudentsByLecture(lecIdx, pageable);
    }

    /* 학생 + 강의 조합으로 수강 정보 조회 */
    public Optional<EnrollmentExtendedTbl> getEnrollmentByStudentAndLecture(Integer studentIdx, Integer lecIdx) {
        return enrollmentRepository.findByStudentIdxAndLecIdx(studentIdx, lecIdx);
    }

    /* 여러 강의의 수강생 목록 일괄 조회 (IN 절 사용) */
    public List<EnrollmentExtendedTbl> getEnrollmentsByLectures(List<Integer> lecIdxList) {
        return enrollmentRepository.findAllByLecIdxIn(lecIdxList);
    }

    /* 여러 학생의 수강 목록 일괄 조회 (IN 절 사용) */
    public List<EnrollmentExtendedTbl> getEnrollmentsByStudents(List<Integer> studentIdxList) {
        return enrollmentRepository.findAllByStudentIdxIn(studentIdxList);
    }

    // ========== 수강신청 등록/수정/삭제 메서드 ==========

    /* 수강신청
     * - 중복 수강신청 방지
     * - 강의 정원 확인
     * - 수강 인원 증가
     */
    @Transactional
    public EnrollmentExtendedTbl enrollStudent(Integer studentIdx, Integer lecIdx) {
        return enrollLecture(studentIdx, lecIdx);
    }

    /* 수강신청 (강의 코드 기반)
     * ✅ lecSerial (강의 코드)로 수강신청
     * - lecSerial을 lecIdx로 변환 후 기존 로직 재사용
     */
    @Transactional
    public EnrollmentExtendedTbl enrollStudentBySerial(Integer studentIdx, String lecSerial) {
        // lecSerial로 강의 조회
        Integer lecIdx = lectureService.getLectureBySerial(lecSerial)
                .map(lecture -> lecture.getLecIdx())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의 코드입니다: " + lecSerial));
        
        // 기존 enrollLecture 로직 재사용
        return enrollLecture(studentIdx, lecIdx);
    }

    /* 수강신청 (내부 메서드)
     * - 중복 수강신청 방지
     * - 강의 정원 확인
     * - 수강 인원 증가
     */
    @Transactional
    public EnrollmentExtendedTbl enrollLecture(Integer studentIdx, Integer lecIdx) {
        // 중복 수강신청 확인
        if (enrollmentRepository.existsByStudentIdxAndLecIdx(studentIdx, lecIdx)) {
            throw new IllegalArgumentException("이미 수강신청한 강의입니다.");
        }

        // 강의 정원 확인
        if (!lectureService.hasAvailableSeats(lecIdx)) {
            throw new IllegalArgumentException("수강 정원이 초과되었습니다.");
        }

        // 강의 존재 여부 확인
        lecTblRepository.findById(lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다: " + lecIdx));

        // 수강신청 정보 생성
        EnrollmentExtendedTbl enrollment = new EnrollmentExtendedTbl();
        enrollment.setStudentIdx(studentIdx);
        enrollment.setLecIdx(lecIdx);
        
        // 초기 JSON 데이터 설정
        String initialData = createInitialEnrollmentData();
        enrollment.setEnrollmentData(initialData);

        // 수강 인원 증가
        if (!lectureService.incrementEnrollment(lecIdx)) {
            throw new IllegalStateException("수강 인원 증가에 실패했습니다.");
        }

        return enrollmentRepository.save(enrollment);
    }

    /* 초기 enrollmentData JSON 생성 */
    private String createInitialEnrollmentData() {
        try {
            Map<String, Object> data = Map.of(
                "enrollment", Map.of(
                    "status", "ENROLLED",
                    "enrollmentDate", getCurrentDateTime()
                ),
                "attendance", List.of(),
                "grade", Map.of()
            );
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /* 수강 취소
     * - 수강 인원 감소
     */
    @Transactional
    public void cancelEnrollment(Integer enrollmentIdx) {
        EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(enrollmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강신청입니다: " + enrollmentIdx));

        // 수강 인원 감소
        lectureService.decrementEnrollment(enrollment.getLecIdx());

        enrollmentRepository.deleteById(enrollmentIdx);
    }

    /* 학생 + 강의 조합으로 수강 취소 */
    @Transactional
    public void cancelEnrollmentByStudentAndLecture(Integer studentIdx, Integer lecIdx) {
        EnrollmentExtendedTbl enrollment = enrollmentRepository
                .findByStudentIdxAndLecIdx(studentIdx, lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("수강신청 정보를 찾을 수 없습니다."));

        // 수강 인원 감소
        lectureService.decrementEnrollment(lecIdx);

        enrollmentRepository.delete(enrollment);
    }

    /* 출석 정보 업데이트 (간단 버전) */
    @Transactional
    public EnrollmentExtendedTbl updateAttendance(Integer enrollmentIdx, 
                                                   Integer attended, Integer absent, Integer late) {
        EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(enrollmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강신청입니다: " + enrollmentIdx));

        try {
            Map<String, Object> currentData = parseEnrollmentData(enrollment.getEnrollmentData());
            Map<String, Object> attendanceInfo = new java.util.HashMap<>();
            if (attended != null) attendanceInfo.put("attended", attended);
            if (absent != null) attendanceInfo.put("absent", absent);
            if (late != null) attendanceInfo.put("late", late);
            attendanceInfo.put("updatedAt", getCurrentDateTime());
            
            currentData.put("attendance", attendanceInfo);
            String jsonData = objectMapper.writeValueAsString(currentData);
            enrollment.setEnrollmentData(jsonData);
            return enrollmentRepository.save(enrollment);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("출석 데이터 변환 실패", e);
        }
    }

    /* 출석 정보 업데이트 (상세 버전) */
    @Transactional
    public EnrollmentExtendedTbl updateAttendanceData(Integer enrollmentIdx, List<Map<String, Object>> attendanceData) {
        EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(enrollmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강신청입니다: " + enrollmentIdx));

        try {
            Map<String, Object> currentData = parseEnrollmentData(enrollment.getEnrollmentData());
            currentData.put("attendance", attendanceData);
            String jsonData = objectMapper.writeValueAsString(currentData);
            enrollment.setEnrollmentData(jsonData);
            return enrollmentRepository.save(enrollment);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("출석 데이터 변환 실패", e);
        }
    }

    /* 성적 정보 업데이트 (간단 버전) */
    @Transactional
    public EnrollmentExtendedTbl updateGrade(Integer enrollmentIdx, String grade, Double score) {
        EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(enrollmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강신청입니다: " + enrollmentIdx));

        try {
            Map<String, Object> currentData = parseEnrollmentData(enrollment.getEnrollmentData());
            Map<String, Object> gradeInfo = new java.util.HashMap<>();
            if (grade != null) gradeInfo.put("letterGrade", grade);
            if (score != null) gradeInfo.put("score", score);
            gradeInfo.put("updatedAt", getCurrentDateTime());
            
            currentData.put("grade", gradeInfo);
            String jsonData = objectMapper.writeValueAsString(currentData);
            enrollment.setEnrollmentData(jsonData);
            return enrollmentRepository.save(enrollment);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("성적 데이터 변환 실패", e);
        }
    }

    /* 성적 정보 업데이트 (상세 버전) */
    @Transactional
    public EnrollmentExtendedTbl updateGradeData(Integer enrollmentIdx, Map<String, Object> gradeData) {
        EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(enrollmentIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강신청입니다: " + enrollmentIdx));

        try {
            Map<String, Object> currentData = parseEnrollmentData(enrollment.getEnrollmentData());
            currentData.put("grade", gradeData);
            String jsonData = objectMapper.writeValueAsString(currentData);
            enrollment.setEnrollmentData(jsonData);
            return enrollmentRepository.save(enrollment);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("성적 데이터 변환 실패", e);
        }
    }

    // ========== JSON 데이터 파싱 메서드 ==========

    /* enrollmentData 전체 JSON 파싱 */
    public Map<String, Object> parseEnrollmentData(String enrollmentJson) {
        if (enrollmentJson == null || enrollmentJson.trim().isEmpty() || "{}".equals(enrollmentJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(enrollmentJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("enrollmentData 파싱 실패", e);
        }
    }

    // ========== 통계 메서드 ==========

    /* 전체 수강신청 수 조회 */
    public long countAllEnrollments() {
        return enrollmentRepository.count();
    }

    /* 강의별 수강생 수 조회 */
    public long countEnrollmentsByLecture(Integer lecIdx) {
        return enrollmentRepository.countByLecIdx(lecIdx);
    }

    /* 학생별 수강 과목 수 조회 */
    public long countEnrollmentsByStudent(Integer studentIdx) {
        return enrollmentRepository.countByStudentIdx(studentIdx);
    }

    /* 특정 강의의 특정 학생 수강 여부 확인 */
    public boolean isEnrolled(Integer studentIdx, Integer lecIdx) {
        return enrollmentRepository.existsByStudentIdxAndLecIdx(studentIdx, lecIdx);
    }

    // ========== 배치 삭제 메서드 ==========

    /* 특정 강의의 모든 수강신청 삭제 */
    @Transactional
    public void deleteAllEnrollmentsByLecture(Integer lecIdx) {
        enrollmentRepository.deleteByLecIdx(lecIdx);
    }

    /* 특정 학생의 모든 수강신청 삭제 */
    @Transactional
    public void deleteAllEnrollmentsByStudent(Integer studentIdx) {
        enrollmentRepository.deleteByStudentIdx(studentIdx);
    }

    /* 여러 강의의 수강신청 일괄 삭제 */
    @Transactional
    public void deleteEnrollmentsByLectures(List<Integer> lecIdxList) {
        List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findAllByLecIdxIn(lecIdxList);
        enrollmentRepository.deleteAll(enrollments);
    }

    // ========== 유틸리티 메서드 ==========

    /* 현재 날짜/시간을 문자열로 반환 (yyyy-MM-dd HH:mm:ss 형식) */
    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ========================================
    // 성적 관리 메서드들
    // ========================================

    /**
     * 성적 구성 설정
     */
    @Transactional
    public Map<String, Object> configureGrade(Map<String, Object> request) {
        // TODO: 성적 구성 설정 로직 구현
        // 임시로 간단한 응답 반환
        return Map.of(
            "lecIdx", request.get("lecIdx"),
            "message", "성적 구성 설정이 완료되었습니다. (구현 예정)"
        );
    }

    /**
     * 학생 성적 정보 조회
     * - 출석 점수 계산
     * - 과제 점수 집계
     * - 총점 및 백분율 계산
     * - ENROLLMENT_DATA JSON 업데이트
     */
    public Map<String, Object> studentGradeInfo(Integer lecIdx, Integer studentIdx) {
        try {
            // 1. 수강신청 정보 조회
            EnrollmentExtendedTbl enrollment = enrollmentRepository.findByStudentIdxAndLecIdx(studentIdx, lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("수강신청 정보를 찾을 수 없습니다."));

            // 2. 기존 JSON 데이터 파싱 (없으면 빈 구조 생성)
            Map<String, Object> enrollmentData = parseEnrollmentData(enrollment.getEnrollmentData());
            Map<String, Object> gradeData = (Map<String, Object>) enrollmentData.computeIfAbsent("grade", k -> new HashMap<>());

            // 3. 출석 점수 계산
            Map<String, Object> attendanceData = calculateAttendanceScore(lecIdx, studentIdx);
            gradeData.put("attendance", attendanceData);

            // 4. 과제 점수 집계
            List<Map<String, Object>> assignmentScores = calculateAssignmentScores(lecIdx, studentIdx);
            gradeData.put("assignments", assignmentScores);

            // 5. 총점 계산
            Map<String, Object> totalData = calculateTotalScore(attendanceData, assignmentScores);
            gradeData.put("total", totalData);

            // 6. JSON 데이터 업데이트
            String updatedJson = objectMapper.writeValueAsString(enrollmentData);
            enrollment.setEnrollmentData(updatedJson);
            enrollmentRepository.save(enrollment);

            // 7. 응답 데이터 구성
            return Map.of(
                "lecIdx", lecIdx,
                "studentIdx", studentIdx,
                "grade", gradeData,
                "updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            throw new RuntimeException("학생 성적 정보 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 출석 점수 계산
     * TODO: AttendanceRepository를 통해 실제 출석 데이터 조회 필요
     */
    private Map<String, Object> calculateAttendanceScore(Integer lecIdx, Integer studentIdx) {
        // 임시 데이터 (실제로는 ATTENDANCE_TBL에서 조회)
        double maxScore = 20.0;
        double currentScore = 18.5;
        double rate = (currentScore / maxScore) * 100;

        return Map.of(
            "maxScore", maxScore,
            "currentScore", currentScore,
            "rate", Math.round(rate * 100.0) / 100.0
        );
    }

    /**
     * 과제 점수 집계
     * TODO: AssignmentRepository를 통해 실제 과제 데이터 조회 필요
     */
    private List<Map<String, Object>> calculateAssignmentScores(Integer lecIdx, Integer studentIdx) {
        // 임시 데이터 (실제로는 ASSIGNMENT_TBL에서 조회)
        return List.of(
            Map.of("name", "과제1", "score", 9, "maxScore", 10),
            Map.of("name", "중간고사", "score", 85, "maxScore", 100),
            Map.of("name", "기말고사", "score", 92, "maxScore", 100)
        );
    }

    /**
     * 총점 계산
     */
    private Map<String, Object> calculateTotalScore(Map<String, Object> attendanceData, 
                                                     List<Map<String, Object>> assignmentScores) {
        // 출석 점수
        double attendanceScore = ((Number) attendanceData.get("currentScore")).doubleValue();
        
        // 과제 점수 합계
        double assignmentScore = assignmentScores.stream()
            .mapToDouble(a -> ((Number) a.get("score")).doubleValue())
            .sum();
        
        // 총 만점
        double attendanceMax = ((Number) attendanceData.get("maxScore")).doubleValue();
        double assignmentMax = assignmentScores.stream()
            .mapToDouble(a -> ((Number) a.get("maxScore")).doubleValue())
            .sum();
        
        // 합계
        double totalScore = attendanceScore + assignmentScore;
        double totalMax = attendanceMax + assignmentMax;
        double percentage = (totalScore / totalMax) * 100;

        return Map.of(
            "score", Math.round(totalScore * 100.0) / 100.0,
            "maxScore", Math.round(totalMax * 100.0) / 100.0,
            "percentage", Math.round(percentage * 100.0) / 100.0
        );
    }

    /**
     * 교수용 성적 조회
     */
    public Map<String, Object> professorGradeView(Integer lecIdx, Integer studentIdx, Integer professorIdx) {
        // TODO: 교수용 성적 조회 로직 구현
        // 임시로 간단한 응답 반환
        return Map.of(
            "lecIdx", lecIdx,
            "studentIdx", studentIdx,
            "professorIdx", professorIdx,
            "message", "교수용 성적 조회가 완료되었습니다. (구현 예정)"
        );
    }

    /**
     * 성적 목록 조회
     */
    public Map<String, Object> gradeList(Integer lecIdx, Pageable pageable, String sortBy, String sortOrder) {
        // TODO: 성적 목록 조회 로직 구현
        // 임시로 간단한 응답 반환
        return Map.of(
            "lecIdx", lecIdx,
            "page", pageable.getPageNumber(),
            "size", pageable.getPageSize(),
            "sortBy", sortBy,
            "sortOrder", sortOrder,
            "message", "성적 목록 조회가 완료되었습니다. (구현 예정)"
        );
    }

    /**
     * 최종 등급 배정
     */
    @Transactional
    public Map<String, Object> finalizeGrades(Integer lecIdx, Double passingThreshold, Map<String, Object> request) {
        // TODO: 최종 등급 배정 로직 구현
        // 임시로 간단한 응답 반환
        return Map.of(
            "lecIdx", lecIdx,
            "passingThreshold", passingThreshold,
            "message", "최종 등급 배정이 완료되었습니다. (구현 예정)"
        );
    }
}
