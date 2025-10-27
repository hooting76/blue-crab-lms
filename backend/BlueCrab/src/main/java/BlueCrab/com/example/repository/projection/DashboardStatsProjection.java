package BlueCrab.com.example.repository.projection;

/**
 * 관리자 대시보드 통계를 위한 Projection Interface
 * Native Query 결과를 매핑하기 위해 Interface로 구현
 */
public interface DashboardStatsProjection {
    Long getTotalReservations();
    Long getPendingCount();
    Long getApprovedCount();
    Long getRejectedCount();
    Long getCancelledCount();
    Long getCompletedCount();
}
