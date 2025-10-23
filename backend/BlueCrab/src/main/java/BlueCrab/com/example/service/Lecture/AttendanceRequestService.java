package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.dto.Lecture.Attendance.*;

import java.util.List;

/**
 * 출석 요청/승인 시스템 Service 인터페이스
 * 
 * 기능:
 * - 학생의 출석 인정 요청 처리
 * - 교수의 출석 승인/반려 처리
 * - 학생 및 교수의 출석 현황 조회
 * - 자동 승인 처리 (7일 경과)
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-23
 */
public interface AttendanceRequestService {
    
    /**
     * 학생의 출석 요청 처리
     * 학생이 특정 강의의 특정 회차에 대해 출석 인정을 요청
     * 
     * @param lecSerial 강의 코드 (LEC_SERIAL)
     * @param sessionNumber 회차 번호 (1~80)
     * @param studentIdx 학생 USER_IDX (JWT에서 추출)
     * @param requestReason 요청 사유 (선택)
     * @return 출석 요청 결과 (현재 출석 데이터 포함)
     */
    AttendanceResponseDto<AttendanceDataDto> requestAttendance(
        String lecSerial,
        Integer sessionNumber,
        Integer studentIdx,
        String requestReason
    );
    
    /**
     * 교수의 출석 승인/반려 처리
     * 교수가 여러 학생의 출석 요청을 한 번에 승인/반려 처리
     * 
     * @param lecSerial 강의 코드 (LEC_SERIAL)
     * @param sessionNumber 회차 번호 (1~80)
     * @param approvalRecords 승인 레코드 배열
     * @param professorIdx 교수 USER_IDX (JWT에서 추출)
     * @return 승인 처리 결과
     */
    AttendanceResponseDto<Void> approveAttendance(
        String lecSerial,
        Integer sessionNumber,
        List<AttendanceApprovalRecordDto> approvalRecords,
        Integer professorIdx
    );
    
    /**
     * 학생의 출석 현황 조회
     * 특정 강의에 대한 학생의 출석 데이터 조회
     * 
     * @param lecSerial 강의 코드 (LEC_SERIAL)
     * @param studentIdx 학생 USER_IDX
     * @return 출석 데이터 (summary, sessions, pendingRequests)
     */
    AttendanceResponseDto<AttendanceDataDto> getStudentAttendance(
        String lecSerial,
        Integer studentIdx
    );
    
    /**
     * 교수의 강의 출석 현황 조회 (전체 학생)
     * 특정 강의의 모든 수강생 출석 현황을 조회
     * 
     * @param lecSerial 강의 코드 (LEC_SERIAL)
     * @param professorIdx 교수 USER_IDX (권한 검증용)
     * @return 학생별 출석 데이터 배열
     */
    AttendanceResponseDto<List<StudentAttendanceDto>> getProfessorAttendance(
        String lecSerial,
        Integer professorIdx
    );
    
    /**
     * 만료된 출석 요청 자동 승인 처리
     * 스케줄러에서 호출되며, 7일 경과한 대기 요청을 자동으로 출석 처리
     * 
     * @return 처리된 요청 수
     */
    int processExpiredRequests();
}
