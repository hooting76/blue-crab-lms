package BlueCrab.com.example.util;

import javax.servlet.http.HttpServletRequest;

/**
 * HTTP 요청 관련 유틸리티 클래스
 */
public class RequestUtils {
    
    /**
     * 클라이언트 IP 주소 추출
     * 프록시, 로드밸런서 환경에서도 실제 클라이언트 IP를 가져옴
     * 
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        String ip = null;
        
        // 프록시 헤더들을 순서대로 확인
        for (String header : headerNames) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 여러 IP가 있는 경우 첫 번째 IP 사용
                if (ip.contains(",")) {
                    ip = ip.substring(0, ip.indexOf(",")).trim();
                }
                break;
            }
        }
        
        // 헤더에서 찾지 못한 경우 직접 연결 IP 사용
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // IPv6 로컬 주소를 IPv4로 변환
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        
        return ip != null ? ip : "unknown";
    }
}
