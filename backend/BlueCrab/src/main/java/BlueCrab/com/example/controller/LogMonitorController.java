package BlueCrab.com.example.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 실시간 로그 모니터링 컨트롤러
 * Server-Sent Events (SSE)를 사용하여 실시간 로그 스트리밍 제공
 */
@Controller
@RequestMapping("/admin/logs")
public class LogMonitorController {
    
    private final ConcurrentHashMap<String, SseEmitter> logClients = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile boolean isMonitoring = false;
    
    /**
     * 로그 모니터링 웹 페이지
     */
    @GetMapping("/monitor")
    public String logMonitorPage() {
        return "log-monitor"; // templates/log-monitor.html
    }
    
    /**
     * 실시간 로그 스트림 (SSE)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter streamLogs(@RequestParam(defaultValue = "app") String logType) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String clientId = "client_" + System.currentTimeMillis();
        
        logClients.put(clientId, emitter);
        
        // 클라이언트 연결 해제 시 정리
        emitter.onCompletion(() -> logClients.remove(clientId));
        emitter.onTimeout(() -> logClients.remove(clientId));
        emitter.onError((ex) -> logClients.remove(clientId));
        
        // 로그 모니터링 시작
        startLogMonitoring(logType);
        
        try {
            // 연결 확인 메시지
            emitter.send(SseEmitter.event()
                .name("connection")
                .data("로그 모니터링 연결됨 - " + logType));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * 로그 파일 모니터링 시작
     */
    private void startLogMonitoring(String logType) {
        if (isMonitoring) return;
        
        isMonitoring = true;
        String logFile = getLogFilePath(logType);
        
        executor.scheduleWithFixedDelay(() -> {
            try {
                watchLogFile(logFile);
            } catch (Exception e) {
                broadcastToClients("error", "로그 모니터링 오류: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    /**
     * 로그 파일 경로 결정
     * 외장 톰캣 환경을 고려한 동적 경로 설정
     */
    private String getLogFilePath(String logType) {
        String baseDir = getLogBaseDirectory();
        
        switch (logType.toLowerCase()) {
            case "auth":
                return baseDir + "/bluecrab-auth.log";
            case "error":
                return baseDir + "/bluecrab-error.log";
            case "app":
            default:
                return baseDir + "/bluecrab-app.log";
        }
    }
    
    /**
     * 로그 베이스 디렉토리 결정
     * 1. 톰캣 환경: $CATALINA_BASE/logs
     * 2. 개발 환경: ./logs
     * 3. 사용자 홈: ~/bluecrab/logs
     */
    private String getLogBaseDirectory() {
        // 1. 톰캣 환경 체크
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null) {
            return catalinaBase + "/logs";
        }
        
        // 2. 개발 환경 체크
        java.io.File devLogs = new java.io.File("logs");
        if (devLogs.exists()) {
            return "logs";
        }
        
        // 3. 사용자 홈 디렉토리
        String userHome = System.getProperty("user.home");
        return userHome + "/bluecrab/logs";
    }
    
    /**
     * 로그 파일 변화 감지 및 전송
     */
    private void watchLogFile(String logFilePath) {
        try {
            Path logPath = Paths.get(logFilePath);
            if (!Files.exists(logPath)) {
                return;
            }
            
            // 파일의 마지막 몇 줄 읽기
            String lastLines = getLastLines(logFilePath, 5);
            if (!lastLines.isEmpty()) {
                broadcastToClients("log", lastLines);
            }
            
        } catch (Exception e) {
            broadcastToClients("error", "로그 파일 읽기 오류: " + e.getMessage());
        }
    }
    
    /**
     * 파일의 마지막 N줄 읽기
     */
    private String getLastLines(String filePath, int lines) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String[] lastLines = new String[lines];
            String line;
            int index = 0;
            
            while ((line = reader.readLine()) != null) {
                lastLines[index % lines] = line;
                index++;
            }
            
            StringBuilder result = new StringBuilder();
            int start = Math.max(0, index - lines);
            for (int i = 0; i < Math.min(lines, index); i++) {
                int lineIndex = (start + i) % lines;
                if (lastLines[lineIndex] != null) {
                    result.append(lastLines[lineIndex]).append("\n");
                }
            }
            
            return result.toString();
        } catch (IOException e) {
            return "";
        }
    }
    
    /**
     * 모든 연결된 클라이언트에 메시지 전송
     */
    private void broadcastToClients(String eventType, String data) {
        logClients.entrySet().removeIf(entry -> {
            try {
                entry.getValue().send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
                return false;
            } catch (IOException e) {
                return true; // 연결 끊어진 클라이언트 제거
            }
        });
    }
    
    /**
     * 현재 로그 상태 조회
     */
    @GetMapping("/status")
    @ResponseBody
    public String getLogStatus() {
        return String.format("연결된 클라이언트: %d개, 모니터링 상태: %s", 
            logClients.size(), isMonitoring ? "활성" : "비활성");
    }
}
