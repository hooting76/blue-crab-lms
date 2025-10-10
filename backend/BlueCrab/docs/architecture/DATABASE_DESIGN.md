# 🗄️ 데이터베이스 설계

## 📋 개요

BlueCrab 프로젝트의 데이터베이스 설계 문서입니다. Oracle Database를 사용하며, Spring Data JPA를 통해 ORM 방식으로 데이터를 관리합니다.

## 🔧 데이터베이스 연결 정보

```properties
# Database Configuration
spring.datasource.url=jdbc:mariadb://121.165.24.26:55511/blue_crab
spring.datasource.username=KDT_project
spring.datasource.password=KDT_project
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# Connection Pool (HikariCP)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

## 📊 테이블 설계

### 1. USER_TBL (사용자 테이블)

사용자의 기본 정보와 인증 정보를 저장하는 메인 테이블입니다.

#### 테이블 구조
| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|----------|------|
| USER_IDX | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 사용자 고유 식별자 |
| USER_EMAIL | VARCHAR(200) | NOT NULL, UNIQUE | 사용자 이메일 (로그인 ID) |
| USER_PW | VARCHAR(200) | NOT NULL | 암호화된 비밀번호 |
| USER_NAME | VARCHAR(50) | NOT NULL | 사용자 실명 |
| USER_PHONE | CHAR(11) | NOT NULL | 휴대폰번호 (하이픈 제외) |
| USER_BIRTH | VARCHAR(100) | NOT NULL | 생년월일 |
| USER_STUDENT | INTEGER | NOT NULL | 사용자 유형 (0: 학생, 1: 교수) |
| USER_LATEST | VARCHAR(100) | NULL | 최근 학력/경력 |
| USER_ZIP | INTEGER | NULL | 우편번호 |
| USER_FIRST_ADD | VARCHAR(200) | NULL | 기본 주소 |
| USER_LAST_ADD | VARCHAR(100) | NULL | 상세 주소 |
| USER_REG | VARCHAR(100) | NULL | 등록일시 |
| USER_REG_IP | VARCHAR(100) | NULL | 등록 시 IP 주소 |

#### DDL
```sql
CREATE TABLE USER_TBL (
    USER_IDX INTEGER PRIMARY KEY AUTO_INCREMENT,
    USER_EMAIL VARCHAR(200) NOT NULL UNIQUE,
    USER_PW VARCHAR(200) NOT NULL,
    USER_NAME VARCHAR(50) NOT NULL,
    USER_PHONE CHAR(11) NOT NULL,
    USER_BIRTH VARCHAR(100) NOT NULL,
    USER_STUDENT INTEGER NOT NULL DEFAULT 1,
    USER_LATEST VARCHAR(100),
    USER_ZIP INTEGER,
    USER_FIRST_ADD VARCHAR(200),
    USER_LAST_ADD VARCHAR(100),
    USER_REG VARCHAR(100),
    USER_REG_IP VARCHAR(100)
);

-- 인덱스 생성
CREATE INDEX idx_user_email ON USER_TBL(USER_EMAIL);
CREATE INDEX idx_user_student ON USER_TBL(USER_STUDENT);
```

#### JPA 엔티티 매핑
```java
@Entity
@Table(name = "USER_TBL")
public class UserTbl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_IDX")
    private Integer userIdx;
    
    @Column(name = "USER_EMAIL", nullable = false, length = 200)
    private String userEmail;
    
    @Column(name = "USER_PW", nullable = false, length = 200)
    private String userPw;
    
    // ... 기타 필드들
}
```

## 🔐 보안 설계

### 비밀번호 암호화
- **암호화 방식**: BCrypt
- **Salt Rounds**: 기본값 사용 (강도 10)
- **저장 형식**: BCrypt 해시 문자열

```java
// 비밀번호 암호화 예시
@Autowired
private PasswordEncoder passwordEncoder;

// 암호화
String encodedPassword = passwordEncoder.encode("rawPassword");

// 검증
boolean matches = passwordEncoder.matches("rawPassword", encodedPassword);
```

### 개인정보 보호
- **이메일**: 로그인 ID로 사용, 중복 불가
- **전화번호**: CHAR(11)로 하이픈 없이 저장
- **주소 정보**: 선택사항으로 NULL 허용
- **등록 IP**: 감사 로그 목적으로 기록

## 📈 성능 최적화

### 인덱스 전략
1. **PRIMARY KEY**: USER_IDX (자동 생성)
2. **UNIQUE INDEX**: USER_EMAIL (로그인 조회용)
3. **일반 INDEX**: USER_STUDENT (사용자 유형별 조회)

### 커넥션 풀 설정
```properties
# HikariCP 설정
spring.datasource.hikari.maximum-pool-size=20  # 최대 연결 수
spring.datasource.hikari.minimum-idle=5        # 최소 유휴 연결
spring.datasource.hikari.idle-timeout=300000   # 유휴 타임아웃 (5분)
spring.datasource.hikari.connection-timeout=20000  # 연결 타임아웃 (20초)
```

## 🔄 데이터 마이그레이션

### JPA DDL 설정
```properties
# 개발 환경
spring.jpa.hibernate.ddl-auto=update  # 스키마 자동 업데이트
spring.jpa.show-sql=false            # SQL 로그 출력 여부
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
```

### 마이그레이션 전략
1. **개발 환경**: `ddl-auto=update` - 자동 스키마 업데이트
2. **운영 환경**: `ddl-auto=none` - 수동 마이그레이션 권장

## 💾 백업 및 복구

### 백업 전략
- **전체 백업**: 매일 자정 수행
- **증분 백업**: 4시간마다 수행
- **보관 정책**: 7일간 보관

### 복구 절차
1. 데이터베이스 서비스 중지
2. 백업 파일에서 복구
3. 애플리케이션 재시작 및 검증

## 📊 모니터링 지표

### 성능 모니터링
- **연결 수**: 현재 활성 연결 개수
- **쿼리 성능**: 느린 쿼리 탐지 (2초 이상)
- **데드락**: 데드락 발생 횟수
- **테이블 사이즈**: 각 테이블의 크기 변화

### 알림 임계값
- **연결 수**: 최대 연결의 80% 초과시
- **응답 시간**: 평균 응답시간 1초 초과시
- **디스크 사용량**: 80% 초과시

## 🚨 장애 대응

### 일반적인 문제점
1. **연결 풀 고갈**
   - 원인: 연결 누수, 과도한 동시 요청
   - 해결: 연결 풀 크기 조정, 커넥션 누수 점검

2. **느린 쿼리**
   - 원인: 인덱스 부족, 비효율적인 쿼리
   - 해결: 인덱스 추가, 쿼리 최적화

3. **데드락**
   - 원인: 동시 트랜잭션 충돌
   - 해결: 트랜잭션 범위 최소화, 락 순서 통일

## 🔮 확장 계획

### 단기 계획
- [ ] 사용자 권한 테이블 추가
- [ ] 로그인 이력 테이블 추가
- [ ] 세션 관리 테이블 추가

### 장기 계획
- [ ] 읽기 전용 DB 분리 (Master-Slave)
- [ ] 파티셔닝 적용 (월별)
- [ ] 캐싱 레이어 도입 (Redis)

---

> 💡 **참고**: 이 문서는 데이터베이스 스키마 변경 시 함께 업데이트되어야 합니다.