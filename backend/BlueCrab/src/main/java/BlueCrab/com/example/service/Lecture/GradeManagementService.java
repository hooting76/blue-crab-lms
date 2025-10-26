// 작성자: 성태준
// 성적 관리 서비스

package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 성적 관리 서비스
 * - 성적 구성 설정
 * - 교수용 성적 조회
 * - 성적 목록 조회
 * - 최종 등급 배정
 */
@Service
@Transactional(readOnly = true)
public class GradeManagementService {

    @Autowired
    private EnrollmentExtendedTblRepository enrollmentRepository;

    @Autowired
    private LecTblRepository lecTblRepository;

    @Autowired
    private GradeCalculationService gradeCalculationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 성적 구성 설정
     * - 강의별 성적 비율 설정 (출석, 과제, 시험)
     * - 등급 분포 기준 설정 (A, B, C, D 비율)
     */
    @Transactional
    public Map<String, Object> configureGrade(Map<String, Object> request) {
        try {
            Integer lecIdx = (Integer) request.get("lecIdx");
            Integer attendanceMaxScore = (Integer) request.get("attendanceMaxScore");
            // assignmentTotalScore는 선택적 (과제 생성 시 자동 계산되므로 기본값 사용)
            Integer assignmentTotalScore = request.get("assignmentTotalScore") != null ? 
                (Integer) request.get("assignmentTotalScore") : null;
            
            // 지각 감점 설정 (교수 재량)
            // 0.0 = 감점 없음 (출석과 동일), 0.5 = 0.5점 감점, 1.0 = 1점 감점 등
            Double latePenaltyPerSession = request.get("latePenaltyPerSession") != null ? 
                ((Number) request.get("latePenaltyPerSession")).doubleValue() : 0.0;
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> gradeDistribution = (Map<String, Integer>) request.get("gradeDistribution");

            // 강의 존재 여부 확인
            if (!lecTblRepository.existsById(lecIdx)) {
                throw new IllegalArgumentException("존재하지 않는 강의입니다: " + lecIdx);
            }

            // 성적 구성 JSON 생성
            Map<String, Object> gradeConfig = new HashMap<>();
            gradeConfig.put("attendanceMaxScore", attendanceMaxScore != null ? attendanceMaxScore : 20);
            gradeConfig.put("assignmentTotalScore", assignmentTotalScore != null ? assignmentTotalScore : 50);
            
            // 지각 감점 설정 추가 (교수 재량)
            gradeConfig.put("latePenaltyPerSession", latePenaltyPerSession);

            // 등급 분포 (기본값: A=30%, B=40%, C=20%, D=10%)
            if (gradeDistribution == null) {
                gradeDistribution = Map.of("A", 30, "B", 40, "C", 20, "D", 10);
            }
            gradeConfig.put("gradeDistribution", gradeDistribution);
            gradeConfig.put("configuredAt", getCurrentDateTime());

            // 총 만점 계산 (출석 + 과제)
            // assignmentTotalScore가 null이면 기본값 사용 (과제 생성 시 동적 계산됨)
            int attendanceScore = (Integer) gradeConfig.get("attendanceMaxScore");
            int assignmentScore = gradeConfig.get("assignmentTotalScore") != null ? 
                (Integer) gradeConfig.get("assignmentTotalScore") : 0;
            int totalMaxScore = attendanceScore + assignmentScore;
            gradeConfig.put("totalMaxScore", totalMaxScore);

            // ✅ DB에 gradeConfig 저장 및 모든 수강생의 성적 재계산
            List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findStudentsByLecture(lecIdx);
            int updatedCount = 0;
            int failedCount = 0;
            
            for (EnrollmentExtendedTbl enrollment : enrollments) {
                try {
                    // 기존 ENROLLMENT_DATA 파싱 (null 안전 처리)
                    Map<String, Object> currentData = parseEnrollmentData(enrollment.getEnrollmentData());
                    if (currentData == null) {
                        currentData = new HashMap<>();
                    }
                    
                    // gradeConfig 병합 (기존 attendance, grade 데이터 유지)
                    currentData.put("gradeConfig", gradeConfig);
                    
                    // JSON 직렬화 및 저장
                    String updatedJson = objectMapper.writeValueAsString(currentData);
                    enrollment.setEnrollmentData(updatedJson);
                    enrollmentRepository.save(enrollment);
                    
                    updatedCount++;
                    
                    // ✅ TODO: 성적 재계산은 별도 API로 분리 (일단 주석 처리)
                    // gradeCalculationService.calculateStudentGrade(lecIdx, enrollment.getStudentIdx());
                    
                } catch (Exception e) {
                    // 개별 학생 처리 실패 시 로그만 남기고 계속 진행
                    System.err.println("학생 성적 처리 실패 (enrollmentIdx: " + enrollment.getEnrollmentIdx() + 
                        ", studentIdx: " + enrollment.getStudentIdx() + "): " + e.getMessage());
                    e.printStackTrace();
                    failedCount++;
                }
            }
            
            // 모든 학생이 실패한 경우에만 예외 발생
            if (failedCount > 0 && updatedCount == 0) {
                throw new RuntimeException("모든 학생의 성적 처리에 실패했습니다. 데이터 구조를 확인하세요.");
            }

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("lecIdx", lecIdx);
            resultData.put("gradeConfig", gradeConfig);
            resultData.put("updatedEnrollments", updatedCount);
            resultData.put("failedEnrollments", failedCount);
            
            return Map.of(
                "success", true,
                "message", "성적 구성이 설정되었습니다." + 
                    (failedCount > 0 ? " (경고: " + failedCount + "명 처리 실패)" : ""),
                "data", resultData
            );

        } catch (IllegalArgumentException e) {
            System.err.println("❌ IllegalArgumentException in configureGrade: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Exception in configureGrade: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "성적 구성 설정 중 오류: " + e.getMessage());
        }
    }

    /**
     * 교수용 성적 조회
     * - 학생 성적 + 추가 통계 (순위, 평균)
     */
    public Map<String, Object> getProfessorGradeView(Integer lecIdx, Integer studentIdx, Integer professorIdx) {
        try {
            if (professorIdx == null) {
                throw new IllegalArgumentException("교수 정보가 필요합니다.");
            }

            // 학생 성적 정보 조회
            Map<String, Object> gradeInfo = gradeCalculationService.calculateStudentGrade(lecIdx, studentIdx);

            // 추가 통계 정보 계산
            Map<String, Object> statistics = calculateAdditionalStats(lecIdx, studentIdx);

            // 응답 데이터 통합
            Map<String, Object> response = new HashMap<>(gradeInfo);
            response.put("professorView", true);
            response.put("statistics", statistics);

            return response;

        } catch (Exception e) {
            return Map.of("success", false, "message", "교수용 성적 조회 오류: " + e.getMessage());
        }
    }

    /**
     * 추가 통계 정보 계산 (순위, 전체 학생 수, 평균 등)
     */
    private Map<String, Object> calculateAdditionalStats(Integer lecIdx, Integer studentIdx) {
        try {
            List<EnrollmentExtendedTbl> allEnrollments = enrollmentRepository.findStudentsByLecture(lecIdx);
            int totalStudents = allEnrollments.size();

            List<Map<String, Object>> allGrades = new java.util.ArrayList<>();
            double totalPercentage = 0.0;

            for (EnrollmentExtendedTbl enrollment : allEnrollments) {
                Map<String, Object> enrollmentData = parseEnrollmentData(enrollment.getEnrollmentData());
                @SuppressWarnings("unchecked")
                Map<String, Object> gradeData = (Map<String, Object>) enrollmentData.get("grade");

                if (gradeData != null && gradeData.containsKey("total")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> totalData = (Map<String, Object>) gradeData.get("total");
                    if (totalData.containsKey("percentage")) {
                        double percentage = ((Number) totalData.get("percentage")).doubleValue();
                        allGrades.add(Map.of(
                            "studentIdx", enrollment.getStudentIdx(),
                            "percentage", percentage
                        ));
                        totalPercentage += percentage;
                    }
                }
            }

            // 순위 계산
            allGrades.sort((a, b) -> Double.compare(
                ((Number) b.get("percentage")).doubleValue(),
                ((Number) a.get("percentage")).doubleValue()
            ));

            int rank = 1;
            for (int i = 0; i < allGrades.size(); i++) {
                if (allGrades.get(i).get("studentIdx").equals(studentIdx)) {
                    rank = i + 1;
                    break;
                }
            }

            double average = totalStudents > 0 ? totalPercentage / totalStudents : 0.0;

            return Map.of(
                "rank", rank,
                "totalStudents", totalStudents,
                "classAverage", Math.round(average * 100.0) / 100.0
            );

        } catch (Exception e) {
            return Map.of("rank", 0, "totalStudents", 0, "classAverage", 0.0, "error", "통계 계산 실패");
        }
    }

    /**
     * 성적 목록 조회
     * - 전체 수강생의 성적 목록 반환
     * - 정렬 기능 (percentage, name, studentId)
     * - 페이징 지원
     */
    public Map<String, Object> getGradeList(Integer lecIdx, Pageable pageable, String sortBy, String sortOrder) {
        try {
            List<EnrollmentExtendedTbl> allEnrollments = enrollmentRepository.findStudentsByLecture(lecIdx);
            List<Map<String, Object>> gradeList = buildGradeList(allEnrollments);

            // 정렬
            sortGradeList(gradeList, sortBy, sortOrder);

            // 순위 매기기
            for (int i = 0; i < gradeList.size(); i++) {
                gradeList.get(i).put("rank", i + 1);
            }

            // 페이징 처리
            Map<String, Object> pagedResult = applyPagination(gradeList, pageable, sortBy, sortOrder);

            return Map.of("success", true, "data", pagedResult);

        } catch (Exception e) {
            return Map.of("success", false, "message", "성적 목록 조회 오류: " + e.getMessage());
        }
    }

    /**
     * 성적 목록 구성
     */
    private List<Map<String, Object>> buildGradeList(List<EnrollmentExtendedTbl> enrollments) {
        List<Map<String, Object>> gradeList = new java.util.ArrayList<>();

        for (EnrollmentExtendedTbl enrollment : enrollments) {
            Map<String, Object> enrollmentData = parseEnrollmentData(enrollment.getEnrollmentData());
            @SuppressWarnings("unchecked")
            Map<String, Object> gradeData = (Map<String, Object>) enrollmentData.get("grade");

            Map<String, Object> studentGrade = new HashMap<>();
            studentGrade.put("studentIdx", enrollment.getStudentIdx());

            // 학생 정보
            if (enrollment.getStudent() != null) {
                studentGrade.put("studentName", enrollment.getStudent().getUserName());
                studentGrade.put("studentId", enrollment.getStudent().getUserCode());
            } else {
                studentGrade.put("studentName", "학생" + enrollment.getStudentIdx());
                studentGrade.put("studentId", "STU" + enrollment.getStudentIdx());
            }

            // 성적 데이터
            if (gradeData != null) {
                studentGrade.put("attendance", gradeData.get("attendance"));
                studentGrade.put("assignments", gradeData.get("assignments"));
                studentGrade.put("total", gradeData.get("total"));
                studentGrade.put("letterGrade", gradeData.get("letterGrade"));

                if (gradeData.containsKey("total")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> totalData = (Map<String, Object>) gradeData.get("total");
                    if (totalData.containsKey("percentage")) {
                        studentGrade.put("percentage", ((Number) totalData.get("percentage")).doubleValue());
                    }
                }
            } else {
                studentGrade.put("percentage", 0.0);
            }

            gradeList.add(studentGrade);
        }

        return gradeList;
    }

    /**
     * 성적 목록 정렬
     */
    private void sortGradeList(List<Map<String, Object>> gradeList, String sortBy, String sortOrder) {
        String sortField = sortBy != null ? sortBy : "percentage";
        boolean ascending = "asc".equalsIgnoreCase(sortOrder);

        gradeList.sort((a, b) -> {
            int result = 0;

            switch (sortField) {
                case "percentage":
                    Double percentageA = a.get("percentage") != null ? ((Number) a.get("percentage")).doubleValue() : 0.0;
                    Double percentageB = b.get("percentage") != null ? ((Number) b.get("percentage")).doubleValue() : 0.0;
                    result = Double.compare(percentageA, percentageB);
                    break;

                case "name":
                    String nameA = a.get("studentName") != null ? (String) a.get("studentName") : "";
                    String nameB = b.get("studentName") != null ? (String) b.get("studentName") : "";
                    result = nameA.compareTo(nameB);
                    break;

                case "studentId":
                    String idA = a.get("studentId") != null ? (String) a.get("studentId") : "";
                    String idB = b.get("studentId") != null ? (String) b.get("studentId") : "";
                    result = idA.compareTo(idB);
                    break;

                default:
                    Double pA = a.get("percentage") != null ? ((Number) a.get("percentage")).doubleValue() : 0.0;
                    Double pB = b.get("percentage") != null ? ((Number) b.get("percentage")).doubleValue() : 0.0;
                    result = Double.compare(pA, pB);
            }

            return ascending ? result : -result;
        });
    }

    /**
     * 페이징 처리
     */
    private Map<String, Object> applyPagination(List<Map<String, Object>> gradeList, 
                                                 Pageable pageable, String sortBy, String sortOrder) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int totalElements = gradeList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<Map<String, Object>> pagedList = fromIndex < totalElements
            ? gradeList.subList(fromIndex, toIndex)
            : List.of();

        return Map.of(
            "content", pagedList,
            "totalElements", totalElements,
            "totalPages", totalPages,
            "currentPage", page,
            "pageSize", size,
            "sortBy", sortBy != null ? sortBy : "percentage",
            "sortOrder", sortOrder != null ? sortOrder : "desc"
        );
    }

    /**
     * 최종 등급 배정
     * - 60% 기준 합격/불합격 분류
     * - 합격자만 상대평가 (A, B, C, D)
     * - 하위 침범 방식
     * - 동점자 처리
     */
    @Transactional
    public Map<String, Object> finalizeGrades(Integer lecIdx, Double passingThreshold, Map<String, Object> request) {
        try {
            List<EnrollmentExtendedTbl> allEnrollments = enrollmentRepository.findStudentsByLecture(lecIdx);

            if (allEnrollments.isEmpty()) {
                return Map.of("success", false, "message", "수강생이 없습니다.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Integer> gradeDistribution = request.containsKey("gradeDistribution")
                ? (Map<String, Integer>) request.get("gradeDistribution")
                : Map.of("A", 30, "B", 40, "C", 20, "D", 10);

            double threshold = passingThreshold != null ? passingThreshold : 60.0;

            // 등급 배정 처리
            GradeFinalizer finalizer = new GradeFinalizer(
                allEnrollments, threshold, gradeDistribution, enrollmentRepository, objectMapper
            );
            
            return finalizer.execute();

        } catch (Exception e) {
            return Map.of("success", false, "message", "등급 배정 오류: " + e.getMessage());
        }
    }

    /**
     * enrollmentData JSON 파싱
     */
    private Map<String, Object> parseEnrollmentData(String enrollmentJson) {
        if (enrollmentJson == null || enrollmentJson.trim().isEmpty() || "{}".equals(enrollmentJson)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(enrollmentJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("enrollmentData 파싱 실패", e);
        }
    }

    /**
     * 현재 날짜/시간 문자열 반환
     */
    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
