package BlueCrab.com.example.service.support;

import java.util.Map;
import java.util.Set;

/**
 * 상담 상태 전환 검증 헬퍼.
 */
public final class StatusTransitionValidator {

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        "PENDING", Set.of("APPROVED", "REJECTED", "CANCELLED"),
        "APPROVED", Set.of("IN_PROGRESS", "CANCELLED"),
        "IN_PROGRESS", Set.of("COMPLETED", "CANCELLED"),
        "REJECTED", Set.of(),
        "CANCELLED", Set.of(),
        "COMPLETED", Set.of()
    );

    private StatusTransitionValidator() {
    }

    public static void validate(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            throw new IllegalArgumentException("상태 값은 필수입니다.");
        }

        Set<String> candidates = VALID_TRANSITIONS.get(currentStatus);
        if (candidates == null) {
            throw new IllegalArgumentException("알 수 없는 상태: " + currentStatus);
        }

        if (!candidates.contains(newStatus)) {
            throw new IllegalStateException(
                String.format("상태 전환 불가: %s -> %s", currentStatus, newStatus)
            );
        }
    }

    public static boolean requiresReason(String status) {
        return Set.of("REJECTED", "CANCELLED").contains(status);
    }

    public static boolean isTerminal(String status) {
        return Set.of("REJECTED", "CANCELLED", "COMPLETED").contains(status);
    }
}
