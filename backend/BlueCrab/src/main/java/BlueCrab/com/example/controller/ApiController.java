package BlueCrab.com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * API 상태 및 정보 조회를 위한 REST 컨트롤러
 * 
 * 주요 기능:
 * - 시스템 헬스체크 및 상태 모니터링
 * - API 연결 및 인증 테스트
 * - 시스템 및 JVM 정보 제공
 * - 데이터베이스 연결 상태 확인
 * 
 * 모든 엔드포인트는 JSON 형식으로 응답하며,
 * 표준화된 응답 구조를 따릅니다.
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * 데이터베이스 연결을 위한 DataSource
     * 헬스체크 시 데이터베이스 연결 상태를 확인하는 데 사용
     */
    @Autowired
    private DataSource dataSource;
    
    /**
     * 시스템 헬스체크 엔드포인트
     * 애플리케이션의 전반적인 건강 상태를 확인하는 데 사용
     * 
     * 검사 항목:
     * - 애플리케이션 상태: 항상 "UP"
     * - 메모리 상태: 사용량이 80% 미만이면 "UP", 초과시 "WARNING"
     * - 데이터베이스 연결: 연결 성공 시 "UP", 실패 시 "DOWN"
     * 
     * 응답 형식:
     * {
     *   "status": "UP" | "DOWN",
     *   "timestamp": "2025-08-27T12:00:00",
     *   "service": "BlueCrab Backend Server",
     *   "version": "1.0.0",
     *   "components": {
     *     "application": "UP",
     *     "memory": "UP" | "WARNING",
     *     "database": "UP" | "DOWN",
     *     "databaseProduct": "Oracle" (성공 시),
     *     "databaseUrl": "jdbc:..." (성공 시),
     *     "databaseError": "에러 메시지" (실패 시)
     *   },
     *   "error": "에러 메시지" (전체 실패 시)
     * }
     * 
     * 사용 사례:
     * - 로드 밸런서 헬스체크
     * - 모니터링 시스템 상태 확인
     * - 배포 후 서비스 정상 동작 확인
     * 
     * @return 시스템 헬스체크 결과
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> components = new HashMap<>();
        
        try {
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "BlueCrab Backend Server");
            health.put("version", "1.0.0");
            
            // 애플리케이션 상태
            components.put("application", "UP");
            
            // 메모리 상태
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            if (usedMemory < maxMemory * 0.8) {
                components.put("memory", "UP");
            } else {
                components.put("memory", "WARNING");
            }
            
            // 데이터베이스 연결 상태
            try (Connection connection = dataSource.getConnection()) {
                components.put("database", "UP");
                components.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
                components.put("databaseUrl", connection.getMetaData().getURL());
            } catch (Exception e) {
                components.put("database", "DOWN");
                components.put("databaseError", e.getMessage());
            }
            
            health.put("components", components);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    /**
     * API 기본 연결 테스트 엔드포인트
     * 클라이언트가 API 서버에 정상적으로 연결할 수 있는지 확인하는 데 사용
     * 
     * 인증 불필요: 공개 엔드포인트로 누구나 접근 가능
     * 
     * 응답 형식:
     * {
     *   "message": "API 테스트 성공",
     *   "timestamp": "2025-08-27T12:00:00",
     *   "status": "OK"
     * }
     * 
     * 사용 사례:
     * - API 서버 연결 테스트
     * - 네트워크 연결 확인
     * - 클라이언트 사이드 헬스체크
     * - 개발/테스트 환경에서 빠른 연결 확인
     * 
     * @return API 연결 테스트 결과
     */
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "API 테스트 성공");
        result.put("timestamp", LocalDateTime.now());
        result.put("status", "OK");
        return result;
    }
    
    /**
     * 시스템 및 JVM 정보 조회 엔드포인트
     * 현재 실행 중인 JVM과 시스템의 상세 정보를 제공
     * 
     * 제공 정보:
     * - JVM 정보: Java 버전, 벤더, JVM 이름
     * - OS 정보: 운영체제 이름과 버전
     * - 메모리 정보: 최대/총/사용/여유 메모리
     * - 프로세서 정보: 사용 가능한 프로세서 코어 수
     * 
     * 응답 형식:
     * {
     *   "javaVersion": "17.0.8",
     *   "javaVendor": "Oracle Corporation",
     *   "osName": "Linux",
     *   "osVersion": "5.4.0-74-generic",
     *   "memory": {
     *     "maxMemory": "1024 MB",
     *     "totalMemory": "256 MB",
     *     "freeMemory": "128 MB",
     *     "usedMemory": "128 MB"
     *   },
     *   "availableProcessors": 4,
     *   "timestamp": "2025-08-27T12:00:00"
     * }
     * 
     * 사용 사례:
     * - 시스템 모니터링 및 진단
     * - 메모리 사용량 분석
     * - JVM 튜닝을 위한 정보 수집
     * - 운영 환경 분석
     * 
     * 보안 고려사항:
     * - 민감한 시스템 정보가 노출될 수 있으므로 운영 환경에서는 접근 제한 권장
     * 
     * @return 시스템 및 JVM 상세 정보
     */
    @GetMapping("/system-info")
    public Map<String, Object> systemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // JVM 정보
        Runtime runtime = Runtime.getRuntime();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        
        // 메모리 정보
        Map<String, Object> memory = new HashMap<>();
        memory.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        memory.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        memory.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        memory.put("usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + " MB");
        info.put("memory", memory);
        
        // 프로세서 정보
        info.put("availableProcessors", runtime.availableProcessors());
        
        info.put("timestamp", LocalDateTime.now());
        
        return info;
    }
    
    /**
     * 인증된 사용자 전용 테스트 엔드포인트
     * JWT 토큰 인증이 정상적으로 작동하는지 확인하는 데 사용
     * 
     * 인증 요구사항:
     * - 유효한 JWT 액세스 토큰이 필요
     * - Authorization 헤더에 "Bearer {token}" 형식으로 전달
     * 
     * 응답 형식:
     * {
     *   "message": "인증된 사용자만 접근 가능한 엔드포인트입니다.",
     *   "timestamp": "2025-08-27T12:00:00",
     *   "user": "사용자명"
     * }
     * 
     * 에러 상황:
     * - 401 Unauthorized: 유효하지 않은 토큰 또는 토큰 없음
     * - 응답: {"success": false, "message": "인증이 필요합니다...", "timestamp": "...}
     * 
     * 사용 사례:
     * - JWT 인증 시스템 동작 확인
     * - 사용자 인증 상태 검증
     * - 프론트엔드에서 인증 상태 확인
     * - API 게이트웨이 헬스체크
     * 
     * @param request HTTP 요청 객체 (사용자 정보 추출용)
     * @return 인증된 사용자 정보와 함께하는 테스트 결과
     */
    @GetMapping("/test-auth")
    public Map<String, Object> testAuth(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "인증된 사용자만 접근 가능한 엔드포인트입니다.");
        result.put("timestamp", LocalDateTime.now());
        result.put("user", request.getUserPrincipal().getName());
        return result;
    }
    
    /**
     * 간단한 연결 테스트 엔드포인트
     * 가장 기본적인 API 연결 상태를 확인하는 데 사용
     * 
     * 특징:
     * - 인증 불필요 (공개 엔드포인트)
     * - 최소한의 응답 데이터
     * - 가장 빠른 응답 속도
     * - 네트워크 연결 및 기본 라우팅 확인용
     * 
     * 응답 형식:
     * {
     *   "message": "pong",
     *   "timestamp": "2025-08-27T12:00:00"
     * }
     * 
     * 사용 사례:
     * - 네트워크 연결 상태 확인
     * - 로드 밸런서 헬스체크 (가벼운 체크용)
     * - CDN 또는 프록시 서버 상태 확인
     * - 모니터링 시스템의 주기적 핑 테스트
     * - 개발 환경에서 빠른 연결 확인
     * 
     * 성능 특징:
     * - 데이터베이스 연결 불필요
     * - 메모리 계산 불필요
     * - 최소한의 객체 생성
     * - 가장 빠른 응답 시간 보장
     * 
     * @return 간단한 연결 확인 응답
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "pong");
        result.put("timestamp", LocalDateTime.now());
        return result;
    }
}
