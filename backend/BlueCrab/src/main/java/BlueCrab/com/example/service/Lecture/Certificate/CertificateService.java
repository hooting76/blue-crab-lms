package BlueCrab.com.example.service.Lecture.Certificate;

import BlueCrab.com.example.dto.Lecture.Certificate.GradeRecord;
import BlueCrab.com.example.dto.Lecture.Certificate.TranscriptResponse;
import BlueCrab.com.example.util.Lecture.Certificate.GradeCalculator;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.exception.Lecture.Certificate.StudentNotFoundException;
import BlueCrab.com.example.exception.Lecture.Certificate.LectureNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 성적확인서 발급 서비스
 * 학생의 전체 성적 이력을 조회하고 성적확인서를 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {
    
    private final EnrollmentExtendedTblRepository enrollmentRepository;
    private final LecTblRepository lecTblRepository;
    private final UserTblRepository userTblRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 학생의 성적확인서 생성
     * 
     * @param studentIdx 학생 IDX
     * @return 성적확인서 응답 객체
     */
    @Transactional(readOnly = true)
    public TranscriptResponse generateTranscript(Integer studentIdx) {
        long startTime = System.currentTimeMillis();
        log.info("성적확인서 생성 시작 - 학생 IDX: {}", studentIdx);
        
        try {
            // 1. 학생 정보 조회
            UserTbl student = userTblRepository.findById(studentIdx)
                .orElseThrow(() -> new StudentNotFoundException(studentIdx));
            
            // 2. 학생의 모든 수강 정보 조회
            List<EnrollmentExtendedTbl> enrollments = enrollmentRepository
                .findByStudentIdx(studentIdx);
            
            if (enrollments.isEmpty()) {
                log.warn("수강 이력이 없습니다 - 학생 IDX: {}", studentIdx);
                return createEmptyTranscript(student);
            }
            
            // 2-1. LazyInitializationException 방지: Lazy 로딩 엔티티 강제 초기화
            log.debug("Lazy 엔티티 강제 로딩 시작 - Enrollment 수: {}", enrollments.size());
            enrollments.forEach(enrollment -> {
                try {
                    // Lecture 엔티티 강제 로딩
                    if (enrollment.getLecture() != null) {
                        enrollment.getLecture().getLecTit();
                        enrollment.getLecture().getLecProf();
                        enrollment.getLecture().getLecSerial();
                    }
                    // Student 엔티티 강제 로딩
                    if (enrollment.getStudent() != null) {
                        enrollment.getStudent().getUserName();
                        enrollment.getStudent().getUserCode();
                    }
                } catch (Exception e) {
                    log.warn("Lazy 엔티티 로딩 실패 - ENROLLMENT_IDX: {}, 에러: {}", 
                        enrollment.getEnrollmentIdx(), e.getMessage());
                }
            });
            log.debug("Lazy 엔티티 강제 로딩 완료");
            
            // 3. 성적 레코드 생성
            List<GradeRecord> gradeRecords = new ArrayList<>();
            
            for (EnrollmentExtendedTbl enrollment : enrollments) {
                try {
                    GradeRecord record = createGradeRecord(enrollment);
                    if (record != null) {
                        gradeRecords.add(record);
                    }
                } catch (Exception e) {
                    log.error("성적 레코드 생성 실패 - ENROLLMENT_IDX: {}, 에러: {}", 
                        enrollment.getEnrollmentIdx(), e.getMessage(), e);
                }
            }
            
            // 4. 학기별로 정렬
            gradeRecords.sort(Comparator
                .comparing(GradeRecord::getYear, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(GradeRecord::getSemester, Comparator.nullsLast(Comparator.naturalOrder())));
            
            // 5. 학기별 통계 계산
            Map<String, TranscriptResponse.SemesterSummary> semesterSummaries = 
                calculateSemesterSummaries(gradeRecords);
            
            // 6. 전체 통계 계산
            TranscriptResponse.OverallSummary overallSummary = 
                calculateOverallSummary(gradeRecords, student);
            
            // 7. 성적확인서 응답 생성
            TranscriptResponse transcript = TranscriptResponse.builder()
                .student(createStudentInfo(student))
                .gradeRecords(gradeRecords)
                .semesterSummaries(semesterSummaries)
                .overallSummary(overallSummary)
                .issuedAt(LocalDateTime.now())
                .certificateNumber(generateCertificateNumber(studentIdx))
                .build();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("성적확인서 생성 완료 - 학생 IDX: {}, 성적 수: {}, 소요시간: {}ms", 
                studentIdx, gradeRecords.size(), duration);
            
            return transcript;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("성적확인서 생성 실패 - 학생 IDX: {}, 소요시간: {}ms, 에러: {}", 
                studentIdx, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * EnrollmentExtendedTbl에서 GradeRecord 생성
     */
    private GradeRecord createGradeRecord(EnrollmentExtendedTbl enrollment) {
        try {
            // Null 체크: enrollment 기본 정보
            if (enrollment == null || enrollment.getLecIdx() == null) {
                log.error("Enrollment 또는 LEC_IDX가 null입니다 - ENROLLMENT_IDX: {}", 
                    enrollment != null ? enrollment.getEnrollmentIdx() : "null");
                return null;
            }
            
            // 강의 정보 조회
            LecTbl lecture = lecTblRepository.findById(enrollment.getLecIdx())
                .orElseThrow(() -> new LectureNotFoundException(enrollment.getLecIdx()));
            
            // 교수 정보 조회 (Null 안전 처리 + String to Integer 변환)
            UserTbl professor = null;
            Integer professorIdx = null;
            if (lecture.getLecProf() != null) {
                professorIdx = parseStringToInteger(lecture.getLecProf());
                if (professorIdx != null) {
                    professor = userTblRepository.findById(professorIdx).orElse(null);
                }
            }
            
            // JSON 데이터 파싱
            String enrollmentDataJson = enrollment.getEnrollmentData();
            if (enrollmentDataJson == null || enrollmentDataJson.trim().isEmpty() 
                || enrollmentDataJson.equals("{}")) {
                // 성적 데이터가 없는 경우 기본 레코드 반환
                log.debug("성적 데이터 없음 - ENROLLMENT_IDX: {}, 기본 레코드 생성", enrollment.getEnrollmentIdx());
                return createBasicGradeRecord(enrollment, lecture, professor);
            }
            
            // JSON 파싱 시도
            JsonNode root;
            try {
                root = objectMapper.readTree(enrollmentDataJson);
            } catch (Exception e) {
                // JSON 미리보기 (최대 100자)
                String jsonPreview = enrollmentDataJson != null && enrollmentDataJson.length() > 100 
                    ? enrollmentDataJson.substring(0, 100) + "..." 
                    : enrollmentDataJson;
                log.error("JSON 파싱 실패 - ENROLLMENT_IDX: {}, LEC_IDX: {}, JSON 미리보기: {}, 에러: {}", 
                    enrollment.getEnrollmentIdx(), 
                    enrollment.getLecIdx(),
                    jsonPreview,
                    e.getMessage(), 
                    e);
                return createBasicGradeRecord(enrollment, lecture, professor);
            }
            
            JsonNode gradeNode = root.path("grade");
            JsonNode totalNode = gradeNode.path("total");
            
            // 성적 정보 추출 (안전한 BigDecimal 변환)
            BigDecimal maxScore = parseSafeBigDecimal(totalNode, "maxScore", "최대점수");
            BigDecimal score = parseSafeBigDecimal(totalNode, "score", "획득점수");
            BigDecimal percentage = parseSafeBigDecimal(totalNode, "percentage", "백분율");
            
            // 출석 정보 추출 (안전한 변환)
            JsonNode attendanceNode = gradeNode.path("attendanceScore");
            BigDecimal attendanceScore = parseSafeBigDecimal(attendanceNode, "currentScore", "출석점수");
            BigDecimal attendanceMaxScore = parseSafeBigDecimal(attendanceNode, "maxScore", "출석만점");
            Integer attendanceRate = parseSafeInteger(attendanceNode, "attendanceRate", "출석률");
            
            // 과제 정보 추출 (안전한 변환)
            BigDecimal assignmentScore = BigDecimal.ZERO;
            BigDecimal assignmentMaxScore = BigDecimal.ZERO;
            JsonNode assignmentsNode = gradeNode.path("assignments");
            
            if (assignmentsNode.isArray()) {
                for (JsonNode assignment : assignmentsNode) {
                    BigDecimal assScore = parseSafeBigDecimal(assignment, "score", "과제점수");
                    BigDecimal assMaxScore = parseSafeBigDecimal(assignment, "maxScore", "과제만점");
                    
                    assignmentScore = assignmentScore.add(assScore);
                    assignmentMaxScore = assignmentMaxScore.add(assMaxScore);
                }
            }
            
            // 학점 등급 계산
            String letterGrade = GradeCalculator.calculateLetterGrade(percentage);
            
            // 성적 상태 판단
            String gradeStatus = determineGradeStatus(percentage, lecture);
            boolean includedInGpa = !letterGrade.equals("F") && gradeStatus.equals("COMPLETED");
            
            return GradeRecord.builder()
                .lecIdx(lecture.getLecIdx())
                .lecSerial(lecture.getLecSerial())
                .lecTitle(lecture.getLecTit())
                .credits(lecture.getLecPoint())
                .professorIdx(professorIdx)  // String을 Integer로 변환한 값 사용
                .professorName(professor != null ? professor.getUserName() : "미배정")
                .year(lecture.getLecYear())
                .semester(lecture.getLecSemester())
                .totalScore(score)
                .maxScore(maxScore)
                .percentage(percentage)
                .letterGrade(letterGrade)
                .attendanceScore(attendanceScore)
                .attendanceMaxScore(attendanceMaxScore)
                .assignmentScore(assignmentScore)
                .assignmentMaxScore(assignmentMaxScore)
                .attendanceRate(attendanceRate)
                .gradeStatus(gradeStatus)
                .includedInGpa(includedInGpa)
                .remarks(null)
                .build();
                
        } catch (Exception e) {
            log.error("성적 레코드 생성 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * 성적 데이터가 없는 경우 기본 레코드 생성
     */
    private GradeRecord createBasicGradeRecord(EnrollmentExtendedTbl enrollment, 
                                              LecTbl lecture, UserTbl professor) {
        // 교수 IDX 안전 변환
        Integer professorIdx = parseStringToInteger(lecture.getLecProf());
        
        return GradeRecord.builder()
            .lecIdx(lecture.getLecIdx())
            .lecSerial(lecture.getLecSerial())
            .lecTitle(lecture.getLecTit())
            .credits(lecture.getLecPoint())
            .professorIdx(professorIdx)  // String을 Integer로 변환한 값 사용
            .professorName(professor != null ? professor.getUserName() : "미배정")
            .year(lecture.getLecYear())
            .semester(lecture.getLecSemester())
            .totalScore(BigDecimal.ZERO)
            .maxScore(BigDecimal.ZERO)
            .percentage(BigDecimal.ZERO)
            .letterGrade("N/A")
            .attendanceScore(BigDecimal.ZERO)
            .attendanceMaxScore(BigDecimal.ZERO)
            .assignmentScore(BigDecimal.ZERO)
            .assignmentMaxScore(BigDecimal.ZERO)
            .attendanceRate(0)
            .gradeStatus("IN_PROGRESS")
            .includedInGpa(false)
            .remarks("성적 미입력")
            .build();
    }
    
    /**
     * 성적 상태 판단
     */
    private String determineGradeStatus(BigDecimal percentage, LecTbl lecture) {
        if (percentage.compareTo(BigDecimal.ZERO) == 0) {
            return "NOT_GRADED";
        }
        
        // 현재 학기인지 확인 (LEC_YEAR, LEC_SEMESTER로 판단)
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        int currentSemester = (currentMonth >= 3 && currentMonth <= 8) ? 1 : 2;
        
        if (lecture.getLecYear() != null && lecture.getLecSemester() != null) {
            if (lecture.getLecYear() < currentYear) {
                return "COMPLETED";
            } else if (lecture.getLecYear() == currentYear) {
                if (lecture.getLecSemester() < currentSemester) {
                    return "COMPLETED";
                } else {
                    return "IN_PROGRESS";
                }
            }
        }
        
        return "IN_PROGRESS";
    }
    
    /**
     * 학기별 통계 계산
     */
    private Map<String, TranscriptResponse.SemesterSummary> calculateSemesterSummaries(
            List<GradeRecord> gradeRecords) {
        
        Map<String, List<GradeRecord>> groupedBySemester = gradeRecords.stream()
            .filter(record -> record.getYear() != null && record.getSemester() != null)
            .collect(Collectors.groupingBy(record -> 
                record.getYear() + "-" + record.getSemester()));
        
        Map<String, TranscriptResponse.SemesterSummary> summaries = new LinkedHashMap<>();
        
        for (Map.Entry<String, List<GradeRecord>> entry : groupedBySemester.entrySet()) {
            String semesterKey = entry.getKey();
            List<GradeRecord> records = entry.getValue();
            
            String[] parts = semesterKey.split("-");
            int year = Integer.parseInt(parts[0]);
            int semester = Integer.parseInt(parts[1]);
            
            int earnedCredits = records.stream()
                .filter(r -> r.getIncludedInGpa())
                .mapToInt(r -> r.getCredits() != null ? r.getCredits() : 0)
                .sum();
            
            int attemptedCredits = records.stream()
                .mapToInt(r -> r.getCredits() != null ? r.getCredits() : 0)
                .sum();
            
            BigDecimal averagePercentage = GradeCalculator.calculateAveragePercentage(records);
            BigDecimal semesterGpa = GradeCalculator.calculateWeightedGpa(records);
            
            TranscriptResponse.SemesterSummary summary = TranscriptResponse.SemesterSummary.builder()
                .semesterKey(semesterKey)
                .year(year)
                .semester(semester)
                .courseCount(records.size())
                .earnedCredits(earnedCredits)
                .attemptedCredits(attemptedCredits)
                .averagePercentage(averagePercentage)
                .semesterGpa(semesterGpa)
                .gradeACount(GradeCalculator.countGradesByLetter(records, "A"))
                .gradeBCount(GradeCalculator.countGradesByLetter(records, "B"))
                .gradeCCount(GradeCalculator.countGradesByLetter(records, "C"))
                .gradeDCount(GradeCalculator.countGradesByLetter(records, "D"))
                .gradeFCount(GradeCalculator.countGradesByLetter(records, "F"))
                .build();
            
            summaries.put(semesterKey, summary);
        }
        
        return summaries;
    }
    
    /**
     * 전체 통계 계산
     */
    private TranscriptResponse.OverallSummary calculateOverallSummary(
            List<GradeRecord> gradeRecords, UserTbl student) {
        
        int totalEarnedCredits = gradeRecords.stream()
            .filter(r -> r.getIncludedInGpa() != null && r.getIncludedInGpa())
            .mapToInt(r -> r.getCredits() != null ? r.getCredits() : 0)
            .sum();
        
        int totalAttemptedCredits = gradeRecords.stream()
            .mapToInt(r -> r.getCredits() != null ? r.getCredits() : 0)
            .sum();
        
        BigDecimal cumulativeGpa = GradeCalculator.calculateWeightedGpa(gradeRecords);
        BigDecimal averagePercentage = GradeCalculator.calculateAveragePercentage(gradeRecords);
        
        // 졸업 요구 학점 (예시: 140학점)
        int requiredCredits = 140;
        int remainingCredits = Math.max(0, requiredCredits - totalEarnedCredits);
        
        BigDecimal completionRate = GradeCalculator.calculateCompletionRate(
            totalEarnedCredits, totalAttemptedCredits);
        
        return TranscriptResponse.OverallSummary.builder()
            .totalCourses(gradeRecords.size())
            .totalEarnedCredits(totalEarnedCredits)
            .totalAttemptedCredits(totalAttemptedCredits)
            .cumulativeGpa(cumulativeGpa)
            .averagePercentage(averagePercentage)
            .requiredCredits(requiredCredits)
            .remainingCredits(remainingCredits)
            .completionRate(completionRate)
            .totalGradeACount(GradeCalculator.countGradesByLetter(gradeRecords, "A"))
            .totalGradeBCount(GradeCalculator.countGradesByLetter(gradeRecords, "B"))
            .totalGradeCCount(GradeCalculator.countGradesByLetter(gradeRecords, "C"))
            .totalGradeDCount(GradeCalculator.countGradesByLetter(gradeRecords, "D"))
            .totalGradeFCount(GradeCalculator.countGradesByLetter(gradeRecords, "F"))
            .rank(null) // TODO: 석차 계산 로직 추가
            .totalStudents(null)
            .rankPercentile(null)
            .build();
    }
    
    /**
     * 학생 정보 DTO 생성
     */
    private TranscriptResponse.StudentInfo createStudentInfo(UserTbl student) {
        // 학번에서 입학년도 추출 (예: 202500101000 -> 2025)
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
            currentGrade = Math.max(1, Math.min(4, currentGrade)); // 1~4학년 범위로 제한
        }
        
        return TranscriptResponse.StudentInfo.builder()
            .studentIdx(student.getUserIdx())
            .studentCode(student.getUserCode())
            .name(student.getUserName())
            .departmentCode(null) // TODO: 학과 정보 추가
            .departmentName(null)
            .grade(currentGrade)
            .admissionYear(admissionYear)
            .build();
    }
    
    /**
     * 빈 성적확인서 생성
     */
    private TranscriptResponse createEmptyTranscript(UserTbl student) {
        return TranscriptResponse.builder()
            .student(createStudentInfo(student))
            .gradeRecords(Collections.emptyList())
            .semesterSummaries(Collections.emptyMap())
            .overallSummary(TranscriptResponse.OverallSummary.builder()
                .totalCourses(0)
                .totalEarnedCredits(0)
                .totalAttemptedCredits(0)
                .cumulativeGpa(BigDecimal.ZERO)
                .averagePercentage(BigDecimal.ZERO)
                .requiredCredits(140)
                .remainingCredits(140)
                .completionRate(BigDecimal.ZERO)
                .totalGradeACount(0)
                .totalGradeBCount(0)
                .totalGradeCCount(0)
                .totalGradeDCount(0)
                .totalGradeFCount(0)
                .build())
            .issuedAt(LocalDateTime.now())
            .certificateNumber(generateCertificateNumber(student.getUserIdx()))
            .build();
    }
    
    /**
     * 성적확인서 발급 번호 생성
     * 형식: TR-{학번}-{YYYYMMDDHHMMSS}
     */
    private String generateCertificateNumber(Integer studentIdx) {
        UserTbl student = userTblRepository.findById(studentIdx).orElse(null);
        String studentCode = (student != null && student.getUserCode() != null) 
            ? student.getUserCode() 
            : String.valueOf(studentIdx);
        
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        return String.format("TR-%s-%s", studentCode, timestamp);
    }
    
    /**
     * JSON에서 BigDecimal 값을 안전하게 파싱
     * NumberFormatException 방지
     * 
     * @param node JSON 노드
     * @param fieldName 필드명
     * @param fieldDescription 필드 설명 (로그용)
     * @return 파싱된 BigDecimal 값 (실패 시 BigDecimal.ZERO)
     */
    private BigDecimal parseSafeBigDecimal(JsonNode node, String fieldName, String fieldDescription) {
        try {
            if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
                return BigDecimal.ZERO;
            }
            
            String valueStr = node.get(fieldName).asText();
            if (valueStr == null || valueStr.trim().isEmpty() || valueStr.equalsIgnoreCase("null")) {
                return BigDecimal.ZERO;
            }
            
            return new BigDecimal(valueStr.trim());
            
        } catch (NumberFormatException e) {
            log.warn("{} 변환 실패 ({}): {}", fieldDescription, fieldName, 
                node.has(fieldName) ? node.get(fieldName).asText() : "null");
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("{} 파싱 중 예외 발생 ({})", fieldDescription, fieldName, e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * JSON에서 Integer 값을 안전하게 파싱
     * NumberFormatException 방지
     * 
     * @param node JSON 노드
     * @param fieldName 필드명
     * @param fieldDescription 필드 설명 (로그용)
     * @return 파싱된 Integer 값 (실패 시 0)
     */
    private Integer parseSafeInteger(JsonNode node, String fieldName, String fieldDescription) {
        try {
            if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
                return 0;
            }
            
            if (node.get(fieldName).isInt()) {
                return node.get(fieldName).asInt();
            }
            
            String valueStr = node.get(fieldName).asText();
            if (valueStr == null || valueStr.trim().isEmpty() || valueStr.equalsIgnoreCase("null")) {
                return 0;
            }
            
            return Integer.parseInt(valueStr.trim());
            
        } catch (NumberFormatException e) {
            log.warn("{} 변환 실패 ({}): {}", fieldDescription, fieldName,
                node.has(fieldName) ? node.get(fieldName).asText() : "null");
            return 0;
        } catch (Exception e) {
            log.error("{} 파싱 중 예외 발생 ({})", fieldDescription, fieldName, e);
            return 0;
        }
    }
    
    /**
     * String을 Integer로 안전하게 변환
     * 
     * @param value 변환할 문자열
     * @return 변환된 Integer (실패 시 null)
     */
    private Integer parseStringToInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("String을 Integer로 변환 실패: {}", value);
            return null;
        }
    }
}
