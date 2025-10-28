package BlueCrab.com.example.util.Lecture.Certificate;

import BlueCrab.com.example.dto.Lecture.Certificate.GradeRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 성적 등급 계산 유틸리티 클래스
 * 백분율 점수를 기반으로 학점 등급을 계산하고 GPA를 산출
 */
public class GradeCalculator {
    
    /**
     * 백분율 점수를 기반으로 학점 등급을 계산
     * 
     * 등급 기준:
     * - A+: 95% 이상
     * - A0: 90% 이상 95% 미만
     * - B+: 85% 이상 90% 미만
     * - B0: 80% 이상 85% 미만
     * - C+: 75% 이상 80% 미만
     * - C0: 70% 이상 75% 미만
     * - D+: 65% 이상 70% 미만
     * - D0: 60% 이상 65% 미만
     * - F:  60% 미만
     * 
     * @param percentage 백분율 점수 (0~100)
     * @return 학점 등급 (A+, A0, B+, B0, C+, C0, D+, D0, F)
     */
    public static String calculateLetterGrade(BigDecimal percentage) {
        if (percentage == null) {
            return "N/A";
        }
        
        double score = percentage.doubleValue();
        
        if (score >= 95.0) {
            return "A+";
        } else if (score >= 90.0) {
            return "A0";
        } else if (score >= 85.0) {
            return "B+";
        } else if (score >= 80.0) {
            return "B0";
        } else if (score >= 75.0) {
            return "C+";
        } else if (score >= 70.0) {
            return "C0";
        } else if (score >= 65.0) {
            return "D+";
        } else if (score >= 60.0) {
            return "D0";
        } else {
            return "F";
        }
    }
    
    /**
     * 학점 등급을 4.5 만점 기준 GPA로 변환
     * 
     * @param letterGrade 학점 등급
     * @return GPA (4.5 만점)
     */
    public static BigDecimal convertToGpa(String letterGrade) {
        if (letterGrade == null || letterGrade.equals("N/A")) {
            return BigDecimal.ZERO;
        }
        
        switch (letterGrade) {
            case "A+":
                return new BigDecimal("4.5");
            case "A0":
                return new BigDecimal("4.0");
            case "B+":
                return new BigDecimal("3.5");
            case "B0":
                return new BigDecimal("3.0");
            case "C+":
                return new BigDecimal("2.5");
            case "C0":
                return new BigDecimal("2.0");
            case "D+":
                return new BigDecimal("1.5");
            case "D0":
                return new BigDecimal("1.0");
            case "F":
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }
    
    /**
     * 여러 과목의 가중 평균 GPA 계산
     * 
     * @param gradeRecords 성적 레코드 목록
     * @return 가중 평균 GPA
     */
    public static BigDecimal calculateWeightedGpa(java.util.List<GradeRecord> gradeRecords) {
        if (gradeRecords == null || gradeRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalWeightedGpa = BigDecimal.ZERO;
        int totalCredits = 0;
        
        for (GradeRecord record : gradeRecords) {
            // Null 체크
            if (record == null) {
                continue;
            }
            
            // F 등급이나 미완료 과목은 GPA 계산에서 제외
            if (record.getIncludedInGpa() != null && !record.getIncludedInGpa()) {
                continue;
            }
            
            BigDecimal gpa = convertToGpa(record.getLetterGrade());
            // gpa가 null일 경우 0으로 처리
            if (gpa == null) {
                gpa = BigDecimal.ZERO;
            }
            
            int credits = record.getCredits() != null ? record.getCredits() : 0;
            
            totalWeightedGpa = totalWeightedGpa.add(
                gpa.multiply(new BigDecimal(credits))
            );
            totalCredits += credits;
        }
        
        if (totalCredits == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalWeightedGpa.divide(
            new BigDecimal(totalCredits), 
            2, 
            RoundingMode.HALF_UP
        );
    }
    
    /**
     * 평균 백분율 점수 계산
     * 
     * @param gradeRecords 성적 레코드 목록
     * @return 평균 백분율 (0~100)
     */
    public static BigDecimal calculateAveragePercentage(java.util.List<GradeRecord> gradeRecords) {
        if (gradeRecords == null || gradeRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalPercentage = BigDecimal.ZERO;
        int count = 0;
        
        for (GradeRecord record : gradeRecords) {
            // Null 체크
            if (record == null || record.getPercentage() == null) {
                continue;
            }
            
            totalPercentage = totalPercentage.add(record.getPercentage());
            count++;
        }
        
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalPercentage.divide(
            new BigDecimal(count), 
            2, 
            RoundingMode.HALF_UP
        );
    }
    
    /**
     * 학점 취득률 계산
     * 
     * @param earnedCredits 취득 학점
     * @param attemptedCredits 신청 학점
     * @return 취득률 (%)
     */
    public static BigDecimal calculateCompletionRate(int earnedCredits, int attemptedCredits) {
        if (attemptedCredits == 0) {
            return BigDecimal.ZERO;
        }
        
        return new BigDecimal(earnedCredits)
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal(attemptedCredits), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 석차 백분위 계산 (상위 몇 %)
     * 
     * @param rank 석차
     * @param totalStudents 전체 학생 수
     * @return 상위 백분율 (%)
     */
    public static BigDecimal calculateRankPercentile(int rank, int totalStudents) {
        if (totalStudents == 0) {
            return BigDecimal.ZERO;
        }
        
        return new BigDecimal(rank)
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal(totalStudents), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 등급별 과목 수 카운트
     * 
     * @param gradeRecords 성적 레코드 목록
     * @param targetGrade 목표 등급 (A, B, C, D, F)
     * @return 해당 등급의 과목 수
     */
    public static int countGradesByLetter(java.util.List<GradeRecord> gradeRecords, String targetGrade) {
        if (gradeRecords == null || gradeRecords.isEmpty() || targetGrade == null) {
            return 0;
        }
        
        return (int) gradeRecords.stream()
            .filter(record -> record != null)  // Null 체크 추가
            .filter(record -> record.getLetterGrade() != null)
            .filter(record -> record.getLetterGrade().startsWith(targetGrade))
            .count();
    }
}
