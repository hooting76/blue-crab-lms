package BlueCrab.com.example.dto;

import java.util.Map;

/**
 * FCM 토큰 통계 응답 DTO
 */
public class FcmStatsResponse {

    private int totalUsers;
    private int registeredUsers;
    private Map<String, Integer> byPlatform;
    private Map<String, Integer> activeTokens; // 30일 이내 사용
    private Map<String, Integer> inactiveTokens; // 90일 이상 미사용

    public FcmStatsResponse() {}

    public FcmStatsResponse(int totalUsers, int registeredUsers, Map<String, Integer> byPlatform,
                           Map<String, Integer> activeTokens, Map<String, Integer> inactiveTokens) {
        this.totalUsers = totalUsers;
        this.registeredUsers = registeredUsers;
        this.byPlatform = byPlatform;
        this.activeTokens = activeTokens;
        this.inactiveTokens = inactiveTokens;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getRegisteredUsers() {
        return registeredUsers;
    }

    public void setRegisteredUsers(int registeredUsers) {
        this.registeredUsers = registeredUsers;
    }

    public Map<String, Integer> getByPlatform() {
        return byPlatform;
    }

    public void setByPlatform(Map<String, Integer> byPlatform) {
        this.byPlatform = byPlatform;
    }

    public Map<String, Integer> getActiveTokens() {
        return activeTokens;
    }

    public void setActiveTokens(Map<String, Integer> activeTokens) {
        this.activeTokens = activeTokens;
    }

    public Map<String, Integer> getInactiveTokens() {
        return inactiveTokens;
    }

    public void setInactiveTokens(Map<String, Integer> inactiveTokens) {
        this.inactiveTokens = inactiveTokens;
    }
}
