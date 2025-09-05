package BlueCrab.com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 설정 정보를 관리하는 설정 클래스
 * application.properties에서 설정값을 자동으로 바인딩
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    /**
     * JWT 관련 설정 객체
     * 토큰 생성, 검증, 만료 시간 등의 설정을 포함
     */
    private Jwt jwt = new Jwt();
    
    /**
     * 보안 관련 설정 객체
     * CORS, 패스워드 인코딩 등의 보안 설정을 포함
     */
    private Security security = new Security();
    
    /**
     * 데이터베이스 연결 관련 설정 객체
     * 커넥션 풀 크기, 타임아웃 등의 DB 설정을 포함
     */
    private Database database = new Database();
    
    /**
     * JWT 설정 객체를 반환
     * 
     * @return JWT 관련 설정 정보
     */
    public Jwt getJwt() {
        return jwt;
    }
    
    /**
     * JWT 설정 객체를 설정
     * 
     * @param jwt JWT 관련 설정 정보
     */
    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }
    
    /**
     * 보안 설정 객체를 반환
     * 
     * @return 보안 관련 설정 정보
     */
    public Security getSecurity() {
        return security;
    }
    
    /**
     * 보안 설정 객체를 설정
     * 
     * @param security 보안 관련 설정 정보
     */
    public void setSecurity(Security security) {
        this.security = security;
    }
    
    /**
     * 데이터베이스 설정 객체를 반환
     * 
     * @return 데이터베이스 관련 설정 정보
     */
    public Database getDatabase() {
        return database;
    }
    
    /**
     * 데이터베이스 설정 객체를 설정
     * 
     * @param database 데이터베이스 관련 설정 정보
     */
    public void setDatabase(Database database) {
        this.database = database;
    }
    
    /**
     * JWT 관련 설정을 담는 내부 클래스
     * JWT 토큰의 생성, 검증, 만료 시간 등을 설정
     */
    public static class Jwt {
        /**
         * JWT 서명에 사용할 비밀 키
         */
        private String secret;
        
        /**
         * 액세스 토큰 만료 시간 (밀리초 단위)
         * 기본값: 15분 (900,000ms)
         */
        private long accessTokenExpiration = 900000L; // 15분 (기본값)
        
        /**
         * 리프레시 토큰 만료 시간 (밀리초 단위)
         * 기본값: 24시간 (86,400,000ms)
         */
        private long refreshTokenExpiration = 86400000L; // 24시간 (기본값)
        
        /**
         * JWT 서명 비밀 키를 반환
         * 
         * @return JWT 서명에 사용할 비밀 키
         */
        public String getSecret() {
            return secret;
        }
        
        /**
         * JWT 서명 비밀 키를 설정
         * 
         * @param secret JWT 서명에 사용할 비밀 키
         */
        public void setSecret(String secret) {
            this.secret = secret;
        }
        
        /**
         * 액세스 토큰 만료 시간을 반환
         * 
         * @return 액세스 토큰 만료 시간 (밀리초)
         */
        public long getAccessTokenExpiration() {
            return accessTokenExpiration;
        }
        
        /**
         * 액세스 토큰 만료 시간을 설정
         * 
         * @param accessTokenExpiration 액세스 토큰 만료 시간 (밀리초)
         */
        public void setAccessTokenExpiration(long accessTokenExpiration) {
            this.accessTokenExpiration = accessTokenExpiration;
        }
        
        /**
         * 리프레시 토큰 만료 시간을 반환
         * 
         * @return 리프레시 토큰 만료 시간 (밀리초)
         */
        public long getRefreshTokenExpiration() {
            return refreshTokenExpiration;
        }
        
        /**
         * 리프레시 토큰 만료 시간을 설정
         * 
         * @param refreshTokenExpiration 리프레시 토큰 만료 시간 (밀리초)
         */
        public void setRefreshTokenExpiration(long refreshTokenExpiration) {
            this.refreshTokenExpiration = refreshTokenExpiration;
        }
    }
    
    /**
     * 보안 관련 설정을 담는 내부 클래스
     * CORS 정책, 패스워드 인코딩 알고리즘 등의 보안 설정을 포함
     */
    public static class Security {
        /**
         * CORS 허용 출처 목록
         * 기본값: http://localhost:3000
         */
        private String[] allowedOrigins = {"http://localhost:3000"};
        
        /**
         * 패스워드 인코딩에 사용할 알고리즘
         * 기본값: SHA-256
         */
        private String passwordEncodingAlgorithm = "SHA-256";
        
        /**
         * CORS 허용 출처 목록을 반환
         * 
         * @return CORS 허용 출처 URL 배열
         */
        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }
        
        /**
         * CORS 허용 출처 목록을 설정
         * 
         * @param allowedOrigins CORS 허용 출처 URL 배열
         */
        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
        
        /**
         * 패스워드 인코딩 알고리즘을 반환
         * 
         * @return 패스워드 인코딩에 사용할 알고리즘 이름
         */
        public String getPasswordEncodingAlgorithm() {
            return passwordEncodingAlgorithm;
        }
        
        /**
         * 패스워드 인코딩 알고리즘을 설정
         * 
         * @param passwordEncodingAlgorithm 패스워드 인코딩에 사용할 알고리즘 이름
         */
        public void setPasswordEncodingAlgorithm(String passwordEncodingAlgorithm) {
            this.passwordEncodingAlgorithm = passwordEncodingAlgorithm;
        }
    }
    
    /**
     * 데이터베이스 연결 관련 설정을 담는 내부 클래스
     * 커넥션 풀 크기, 타임아웃, 재시도 설정 등의 DB 연결 설정을 포함
     */
    public static class Database {
        /**
         * 최대 커넥션 풀 크기
         * 기본값: 10개
         */
        private int maxPoolSize = 10;
        
        /**
         * 최소 커넥션 풀 크기
         * 기본값: 2개
         */
        private int minPoolSize = 2;
        
        /**
         * 커넥션 타임아웃 시간 (밀리초)
         * 기본값: 30초 (30,000ms)
         */
        private long connectionTimeout = 30000L; // 30초 (기본값)
        
        /**
         * 쿼리 타임아웃 시간 (밀리초)
         * 기본값: 60초 (60,000ms)
         */
        private long queryTimeout = 60000L; // 60초 (기본값)
        
        /**
         * 최대 커넥션 풀 크기를 반환
         * 
         * @return 최대 커넥션 풀 크기
         */
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        /**
         * 최대 커넥션 풀 크기를 설정
         * 
         * @param maxPoolSize 최대 커넥션 풀 크기
         */
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        /**
         * 최소 커넥션 풀 크기를 반환
         * 
         * @return 최소 커넥션 풀 크기
         */
        public int getMinPoolSize() {
            return minPoolSize;
        }
        
        /**
         * 최소 커넥션 풀 크기를 설정
         * 
         * @param minPoolSize 최소 커넥션 풀 크기
         */
        public void setMinPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
        }
        
        /**
         * 커넥션 타임아웃 시간을 반환
         * 
         * @return 커넥션 타임아웃 시간 (밀리초)
         */
        public long getConnectionTimeout() {
            return connectionTimeout;
        }
        
        /**
         * 커넥션 타임아웃 시간을 설정
         * 
         * @param connectionTimeout 커넥션 타임아웃 시간 (밀리초)
         */
        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        /**
         * 쿼리 타임아웃 시간을 반환
         * 
         * @return 쿼리 타임아웃 시간 (밀리초)
         */
        public long getQueryTimeout() {
            return queryTimeout;
        }
        
        /**
         * 쿼리 타임아웃 시간을 설정
         * 
         * @param queryTimeout 쿼리 타임아웃 시간 (밀리초)
         */
        public void setQueryTimeout(long queryTimeout) {
            this.queryTimeout = queryTimeout;
        }
    }
}