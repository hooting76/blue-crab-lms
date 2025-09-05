package BlueCrab.com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 시스템 메트릭 및 헬스체크 API
 */
@RestController
@RequestMapping("/admin/metrics")
public class MetricsController {
    
    /**
     * 시스템 메트릭 조회
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM 메모리 정보
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new HashMap<>();
        memory.put("heap_used", memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024 + " MB");
        memory.put("heap_max", memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024 + " MB");
        memory.put("non_heap_used", memoryBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024 + " MB");
        
        // 시스템 정보
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> system = new HashMap<>();
        system.put("available_processors", runtime.availableProcessors());
        system.put("total_memory", runtime.totalMemory() / 1024 / 1024 + " MB");
        system.put("free_memory", runtime.freeMemory() / 1024 / 1024 + " MB");
        system.put("max_memory", runtime.maxMemory() / 1024 / 1024 + " MB");
        
        // 디스크 정보
        String logBaseDir = getLogBaseDirectory();
        File logDir = new File(logBaseDir);
        Map<String, Object> disk = new HashMap<>();
        if (logDir.exists()) {
            disk.put("log_directory", logBaseDir);
            disk.put("log_directory_size", calculateDirectorySize(logDir) / 1024 / 1024 + " MB");
            disk.put("log_files_count", countFiles(logDir));
        } else {
            disk.put("log_directory", logBaseDir + " (존재하지 않음)");
        }
        disk.put("free_space", new File(".").getFreeSpace() / 1024 / 1024 / 1024 + " GB");
        
        // 애플리케이션 정보
        Map<String, Object> app = new HashMap<>();
        app.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + " seconds");
        app.put("start_time", new java.util.Date(ManagementFactory.getRuntimeMXBean().getStartTime()));
        app.put("jvm_version", System.getProperty("java.version"));
        
        metrics.put("memory", memory);
        metrics.put("system", system);
        metrics.put("disk", disk);
        metrics.put("application", app);
        
        return metrics;
    }
    
    /**
     * 로그 파일 상태 조회
     */
    @GetMapping("/logs")
    public Map<String, Object> getLogMetrics() {
        Map<String, Object> logMetrics = new HashMap<>();
        
        String[] logFiles = {"bluecrab-app.log", "bluecrab-auth.log", "bluecrab-error.log"};
        String logBaseDir = getLogBaseDirectory();
        
        for (String logFile : logFiles) {
            File file = new File(logBaseDir + "/" + logFile);
            Map<String, Object> fileInfo = new HashMap<>();
            
            if (file.exists()) {
                fileInfo.put("path", file.getAbsolutePath());
                fileInfo.put("size", file.length() / 1024 + " KB");
                fileInfo.put("last_modified", new java.util.Date(file.lastModified()));
                fileInfo.put("lines", countLines(file));
            } else {
                fileInfo.put("status", "파일이 존재하지 않습니다");
                fileInfo.put("expected_path", file.getAbsolutePath());
            }
            
            logMetrics.put(logFile, fileInfo);
        }
        
        return logMetrics;
    }
    
    /**
     * 디렉토리 크기 계산
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }
    
    /**
     * 디렉토리 내 파일 개수 계산
     */
    private int countFiles(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * 파일 라인 수 계산 (간략)
     */
    private int countLines(File file) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            int lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            return lines;
        } catch (Exception e) {
            return -1;
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
        File devLogs = new File("logs");
        if (devLogs.exists()) {
            return "logs";
        }
        
        // 3. 사용자 홈 디렉토리
        String userHome = System.getProperty("user.home");
        return userHome + "/bluecrab/logs";
    }
}
