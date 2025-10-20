// 작성자: 성태준
// 성적 업데이트 이벤트 리스너

package BlueCrab.com.example.listener.Lecture;

import BlueCrab.com.example.event.Lecture.GradeUpdateEvent;
import BlueCrab.com.example.service.Lecture.GradeCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 성적 업데이트 이벤트 리스너
 * 출석 체크 또는 과제 채점 시 성적을 자동으로 재계산
 * 
 * 특징:
 * - @Async: 비동기 처리로 메인 로직 성능 영향 최소화
 * - @EventListener: Spring 이벤트 자동 구독
 * - 선별적 재계산: 해당 학생의 성적만 재계산
 */
@Component
@Slf4j
public class GradeUpdateEventListener {

    @Autowired
    private GradeCalculationService gradeCalculationService;

    /**
     * 성적 업데이트 이벤트 처리
     * 
     * @param event 성적 업데이트 이벤트
     */
    @EventListener
    @Async
    public void handleGradeUpdateEvent(GradeUpdateEvent event) {
        try {
            log.info("성적 재계산 시작: {}", event);

            Integer lecIdx = event.getLecIdx();
            Integer studentIdx = event.getStudentIdx();
            String updateType = event.getUpdateType();

            // 성적 재계산 수행
            gradeCalculationService.calculateStudentGrade(lecIdx, studentIdx);

            log.info("성적 재계산 완료: lecIdx={}, studentIdx={}, updateType={}", 
                    lecIdx, studentIdx, updateType);

        } catch (Exception e) {
            log.error("성적 재계산 실패: {}", event, e);
            // 비동기 처리이므로 예외를 던지지 않고 로깅만 수행
            // 메인 로직(출석 체크, 과제 채점)에는 영향 없음
        }
    }

    /**
     * 특정 강의의 전체 학생 성적 재계산 (일괄 처리)
     * 
     * @param lecIdx 강의 IDX
     */
    @Async
    public void recalculateAllGrades(Integer lecIdx) {
        try {
            log.info("전체 학생 성적 재계산 시작: lecIdx={}", lecIdx);
            
            // TODO: 전체 수강생 조회 후 일괄 재계산
            // List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findStudentsByLecture(lecIdx);
            // for (EnrollmentExtendedTbl enrollment : enrollments) {
            //     gradeCalculationService.calculateStudentGrade(lecIdx, enrollment.getStudentIdx());
            // }
            
            log.info("전체 학생 성적 재계산 완료: lecIdx={}", lecIdx);

        } catch (Exception e) {
            log.error("전체 학생 성적 재계산 실패: lecIdx={}", lecIdx, e);
        }
    }
}
