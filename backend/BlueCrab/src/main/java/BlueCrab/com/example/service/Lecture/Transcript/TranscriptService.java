package BlueCrab.com.example.service.Lecture.Transcript;

import BlueCrab.com.example.dto.Lecture.Transcript.CourseDto;
import BlueCrab.com.example.dto.Lecture.Transcript.TranscriptResponseDto;
import BlueCrab.com.example.dto.Lecture.Transcript.TranscriptResponseDto.OverallSummary;
import BlueCrab.com.example.dto.Lecture.Transcript.TranscriptResponseDto.SemesterSummary;
import BlueCrab.com.example.dto.Lecture.Transcript.TranscriptResponseDto.StudentInfo;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 성적확인서 서비스
 * 학생의 전체 성적 이력 조회 및 통계 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {
    
    private final EnrollmentExtendedTblRepository enrollmentRepository;
    private final LecTblRepository lecTblRepository;
    private final UserTblRepository userTblRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 학생 성적확인서 생성
     * 
     * @param studentIdx 학생 IDX
     * @return 성적확인서
     */
    @Transactional(readOnly = true)
    public TranscriptResponseDto generateTranscript(Integer studentIdx) {
        long startTime = System.currentTimeMillis();
        log.info("성적확인서 생성 시작 - 학생 IDX: {}", studentIdx);
        
        try {
            // 1. 학생 정보 조회
            UserTbl student = userTblRepository.findById(studentIdx)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다: " + studentIdx));
            
            // 2. 수강 이력 조회
            List<EnrollmentExtendedTbl> enrollments = enrollmentRepository
                .findByStudentIdx(studentIdx);
            
            if (enrollments.isEmpty()) {
                log.warn("수강 이력이 없습니다 - 학생 IDX: {}", studentIdx);
                return createEmptyTranscript(student);
            }
            
            // 3. 과목별 성적 변환
            List<CourseDto> courses = new ArrayList<>();
            for (EnrollmentExtendedTbl enrollment : enrollments) {
                try {
                    CourseDto course = createCourseDto(enrollment);
                    if (course != null) {
                        courses.add(course);
                    }
                } catch (Exception e) {
                    log.error("과목 정보 생성 실패 - ENROLLMENT_IDX: {}, 에러: {}", 
                        enrollment.getEnrollmentIdx(), e.getMessage(), e);
                }
            }
            
            // 4. 학기별 정렬 (년도 → 학기 순)
            courses.sort(Comparator
                .comparing(CourseDto::getYear, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CourseDto::getSemester, Comparator.nullsLast(Comparator.naturalOrder())));
            
            // 5. 학기별 통계 계산
            Map<String, SemesterSummary> semesterSummaries = calculateSemesterSummaries(courses);
            
            // 6. 전체 통계 계산
            OverallSummary overallSummary = calculateOverallSummary(courses);
            
            // 7. 성적확인서 생성
            TranscriptResponseDto transcript = TranscriptResponseDto.builder()
                .student(createStudentInfo(student))
                .courses(courses)
                .semesterSummaries(semesterSummaries)
                .overallSummary(overallSummary)
                .issuedAt(LocalDateTime.now())
                .certificateNumber(generateCertificateNumber(student))
                .build();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("성적확인서 생성 완료 - 학생 IDX: {}, 과목 수: {}, 소요시간: {}ms", 
                studentIdx, courses.size(), duration);
            
            return transcript;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("성적확인서 생성 실패 - 학생 IDX: {}, 소요시간: {}ms, 에러: {}", 
                studentIdx, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * EnrollmentExtendedTbl → CourseDto 변환
     */
    private CourseDto createCourseDto(EnrollmentExtendedTbl enrollment) {
        try {
            // 강의 정보 조회
            LecTbl lecture = lecTblRepository.findById(enrollment.getLecIdx())
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다: " + enrollment.getLecIdx()));
            
            // 교수 정보 조회
            UserTbl professor = null;
            if (lecture.getLecProf() != null) {
                try {
                    Integer profIdx = Integer.parseInt(lecture.getLecProf().trim());
                    professor = userTblRepository.findById(profIdx).orElse(null);
                } catch (NumberFormatException e) {
                    log.warn("교수 IDX 파싱 실패: {}", lecture.getLecProf());
                }
            }
            
            // JSON 데이터 파싱
            String enrollmentDataJson = enrollment.getEnrollmentData();
            if (enrollmentDataJson == null || enrollmentDataJson.trim().isEmpty() 
                || enrollmentDataJson.equals("{}")) {
                // 성적 데이터 없음 - 기본 정보만 반환
                return createBasicCourseDto(lecture, professor);
            }
            
            JsonNode root = objectMapper.readTree(enrollmentDataJson);
            JsonNode gradeNode = root.path("grade");
            
            // 성적 정보 추출
            JsonNode totalNode = gradeNode.path("total");
            double totalScore = parseDouble(totalNode, "score", 0.0);
            double maxScore = parseDouble(totalNode, "maxScore", 0.0);
            double percentage = parseDouble(totalNode, "percentage", 0.0);
            
            // 최종 성적 및 상태 판정
            boolean finalized = gradeNode.path("finalized").asBoolean(false);
            String letterGrade = "N/A";
            String status = "IN_PROGRESS";
            Integer rank = null;
            Integer totalStudents = null;
            
            if (finalized) {
                // 성적 확정됨
                letterGrade = gradeNode.path("letterGrade").asText("N/A");
                status = letterGrade.equals("F") ? "FAILED" : "COMPLETED";
                
                if (gradeNode.has("rank") && !gradeNode.path("rank").isNull()) {
                    rank = gradeNode.path("rank").asInt();
                }
                if (gradeNode.has("totalStudents") && !gradeNode.path("totalStudents").isNull()) {
                    totalStudents = gradeNode.path("totalStudents").asInt();
                }
            } else {
                // 성적 미확정
                if (percentage >= 60.0) {
                    status = "IN_PROGRESS";
                    letterGrade = "진행중";
                } else {
                    status = "NOT_GRADED";
                    letterGrade = "미확정";
                }
            }
            
            // GPA 계산
            double gpa = convertToGpa(letterGrade);
            
            // 출석 정보 추출
            JsonNode attendanceNode = gradeNode.path("attendanceScore");
            double attendanceScore = parseDouble(attendanceNode, "currentScore", 0.0);
            double attendanceMaxScore = parseDouble(attendanceNode, "maxScore", 0.0);
            int attendanceRate = parseInt(attendanceNode, "attendanceRate", 0);
            
            // 과제 정보 집계
            double assignmentScore = 0.0;
            double assignmentMaxScore = 0.0;
            JsonNode assignmentsNode = gradeNode.path("assignments");
            
            if (assignmentsNode.isArray()) {
                for (JsonNode assignment : assignmentsNode) {
                    assignmentScore += parseDouble(assignment, "score", 0.0);
                    assignmentMaxScore += parseDouble(assignment, "maxScore", 0.0);
                }
            }
            
            return CourseDto.builder()
                .lecIdx(lecture.getLecIdx())
                .lecSerial(lecture.getLecSerial())
                .lecTitle(lecture.getLecTit())
                .credits(lecture.getLecPoint())
                .professorName(professor != null ? professor.getUserName() : "미배정")
                .year(lecture.getLecYear())
                .semester(lecture.getLecSemester())
                .totalScore(round(totalScore))
                .maxScore(round(maxScore))
                .percentage(round(percentage))
                .letterGrade(letterGrade)
                .gpa(gpa)
                .rank(rank)
                .totalStudents(totalStudents)
                .status(status)
                .attendanceScore(round(attendanceScore))
                .attendanceMaxScore(round(attendanceMaxScore))
                .attendanceRate(attendanceRate)
                .assignmentScore(round(assignmentScore))
                .assignmentMaxScore(round(assignmentMaxScore))
                .build();
                
        } catch (Exception e) {
            log.error("CourseDto 생성 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * 성적 데이터 없는 경우 기본 CourseDto 생성
     */
    private CourseDto createBasicCourseDto(LecTbl lecture, UserTbl professor) {
        return CourseDto.builder()
            .lecIdx(lecture.getLecIdx())
            .lecSerial(lecture.getLecSerial())
            .lecTitle(lecture.getLecTit())
            .credits(lecture.getLecPoint())
            .professorName(professor != null ? professor.getUserName() : "미배정")
            .year(lecture.getLecYear())
            .semester(lecture.getLecSemester())
            .totalScore(0.0)
            .maxScore(0.0)
            .percentage(0.0)
            .letterGrade("N/A")
            .gpa(0.0)
            .status("NOT_GRADED")
            .build();
    }
    
    /**
     * 학기별 통계 계산
     */
    private Map<String, SemesterSummary> calculateSemesterSummaries(List<CourseDto> courses) {
        Map<String, List<CourseDto>> groupedBySemester = courses.stream()
            .filter(c -> c.getYear() != null && c.getSemester() != null)
            .collect(Collectors.groupingBy(c -> c.getYear() + "-" + c.getSemester()));
        
        Map<String, SemesterSummary> summaries = new LinkedHashMap<>();
        
        for (Map.Entry<String, List<CourseDto>> entry : groupedBySemester.entrySet()) {
            String semesterKey = entry.getKey();
            List<CourseDto> semesterCourses = entry.getValue();
            
            String[] parts = semesterKey.split("-");
            int year = Integer.parseInt(parts[0]);
            int semester = Integer.parseInt(parts[1]);
            
            // 취득 학점 (COMPLETED만)
            int earnedCredits = semesterCourses.stream()
                .filter(c -> "COMPLETED".equals(c.getStatus()))
                .mapToInt(c -> c.getCredits() != null ? c.getCredits() : 0)
                .sum();
            
            // 신청 학점 (전체)
            int attemptedCredits = semesterCourses.stream()
                .mapToInt(c -> c.getCredits() != null ? c.getCredits() : 0)
                .sum();
            
            // 학기 GPA
            double semesterGpa = calculateWeightedGpa(semesterCourses);
            
            // 평균 백분율
            double averagePercentage = semesterCourses.stream()
                .mapToDouble(c -> c.getPercentage() != null ? c.getPercentage() : 0.0)
                .average()
                .orElse(0.0);
            
            SemesterSummary summary = SemesterSummary.builder()
                .semesterKey(semesterKey)
                .year(year)
                .semester(semester)
                .courseCount(semesterCourses.size())
                .earnedCredits(earnedCredits)
                .attemptedCredits(attemptedCredits)
                .semesterGpa(round(semesterGpa))
                .averagePercentage(round(averagePercentage))
                .gradeACount(countByGrade(semesterCourses, "A"))
                .gradeBCount(countByGrade(semesterCourses, "B"))
                .gradeCCount(countByGrade(semesterCourses, "C"))
                .gradeDCount(countByGrade(semesterCourses, "D"))
                .gradeFCount(countByGrade(semesterCourses, "F"))
                .build();
            
            summaries.put(semesterKey, summary);
        }
        
        return summaries;
    }
    
    /**
     * 전체 통계 계산
     */
    private OverallSummary calculateOverallSummary(List<CourseDto> courses) {
        // 총 취득 학점 (COMPLETED만)
        int totalEarnedCredits = courses.stream()
            .filter(c -> "COMPLETED".equals(c.getStatus()))
            .mapToInt(c -> c.getCredits() != null ? c.getCredits() : 0)
            .sum();
        
        // 총 신청 학점
        int totalAttemptedCredits = courses.stream()
            .mapToInt(c -> c.getCredits() != null ? c.getCredits() : 0)
            .sum();
        
        // 누적 GPA
        double cumulativeGpa = calculateWeightedGpa(courses);
        
        // 평균 백분율
        double averagePercentage = courses.stream()
            .mapToDouble(c -> c.getPercentage() != null ? c.getPercentage() : 0.0)
            .average()
            .orElse(0.0);
        
        // 학점 취득률
        double completionRate = totalAttemptedCredits > 0 
            ? (double) totalEarnedCredits / totalAttemptedCredits * 100.0 
            : 0.0;
        
        return OverallSummary.builder()
            .totalCourses(courses.size())
            .totalEarnedCredits(totalEarnedCredits)
            .totalAttemptedCredits(totalAttemptedCredits)
            .cumulativeGpa(round(cumulativeGpa))
            .averagePercentage(round(averagePercentage))
            .completionRate(round(completionRate))
            .totalGradeACount(countByGrade(courses, "A"))
            .totalGradeBCount(countByGrade(courses, "B"))
            .totalGradeCCount(countByGrade(courses, "C"))
            .totalGradeDCount(countByGrade(courses, "D"))
            .totalGradeFCount(countByGrade(courses, "F"))
            .build();
    }
    
    /**
     * 가중 평균 GPA 계산
     * COMPLETED 과목만 포함
     */
    private double calculateWeightedGpa(List<CourseDto> courses) {
        double totalWeighted = 0.0;
        int totalCredits = 0;
        
        for (CourseDto course : courses) {
            if ("COMPLETED".equals(course.getStatus()) && course.getGpa() != null) {
                int credits = course.getCredits() != null ? course.getCredits() : 0;
                totalWeighted += course.getGpa() * credits;
                totalCredits += credits;
            }
        }
        
        return totalCredits > 0 ? totalWeighted / totalCredits : 0.0;
    }
    
    /**
     * 학점 등급 → GPA 변환 (4.0 만점)
     */
    private double convertToGpa(String letterGrade) {
        if (letterGrade == null) return 0.0;
        
        switch (letterGrade) {
            case "A": return 4.0;
            case "B": return 3.0;
            case "C": return 2.0;
            case "D": return 1.0;
            case "F": return 0.0;
            default: return 0.0;
        }
    }
    
    /**
     * 특정 등급 과목 수 카운트
     */
    private int countByGrade(List<CourseDto> courses, String grade) {
        return (int) courses.stream()
            .filter(c -> grade.equals(c.getLetterGrade()))
            .count();
    }
    
    /**
     * 학생 정보 생성
     */
    private StudentInfo createStudentInfo(UserTbl student) {
        // 학번에서 입학년도 추출 (예: 202500106114 → 2025)
        Integer admissionYear = null;
        if (student.getUserCode() != null && student.getUserCode().length() >= 4) {
            try {
                admissionYear = Integer.parseInt(student.getUserCode().substring(0, 4));
            } catch (NumberFormatException e) {
                log.warn("입학년도 파싱 실패: {}", student.getUserCode());
            }
        }
        
        // 현재 학년 계산
        Integer currentGrade = null;
        if (admissionYear != null) {
            int currentYear = LocalDateTime.now().getYear();
            currentGrade = currentYear - admissionYear + 1;
            currentGrade = Math.max(1, Math.min(4, currentGrade)); // 1~4학년 제한
        }
        
        return StudentInfo.builder()
            .studentIdx(student.getUserIdx())
            .studentCode(student.getUserCode())
            .name(student.getUserName())
            .admissionYear(admissionYear)
            .currentGrade(currentGrade)
            .build();
    }
    
    /**
     * 빈 성적확인서 생성
     */
    private TranscriptResponseDto createEmptyTranscript(UserTbl student) {
        return TranscriptResponseDto.builder()
            .student(createStudentInfo(student))
            .courses(Collections.emptyList())
            .semesterSummaries(Collections.emptyMap())
            .overallSummary(OverallSummary.builder()
                .totalCourses(0)
                .totalEarnedCredits(0)
                .totalAttemptedCredits(0)
                .cumulativeGpa(0.0)
                .averagePercentage(0.0)
                .completionRate(0.0)
                .totalGradeACount(0)
                .totalGradeBCount(0)
                .totalGradeCCount(0)
                .totalGradeDCount(0)
                .totalGradeFCount(0)
                .build())
            .issuedAt(LocalDateTime.now())
            .certificateNumber(generateCertificateNumber(student))
            .build();
    }
    
    /**
     * 성적확인서 발급 번호 생성
     * 형식: TR-{학번}-{YYYYMMDDHHMMSS}
     */
    private String generateCertificateNumber(UserTbl student) {
        String studentCode = student.getUserCode() != null 
            ? student.getUserCode() 
            : String.valueOf(student.getUserIdx());
        
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        return String.format("TR-%s-%s", studentCode, timestamp);
    }
    
    // === 유틸리티 메서드 ===
    
    private double parseDouble(JsonNode node, String fieldName, double defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }
        return node.get(fieldName).asDouble(defaultValue);
    }
    
    private int parseInt(JsonNode node, String fieldName, int defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }
        return node.get(fieldName).asInt(defaultValue);
    }
    
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
