# 🛠️ 개발 환경 설정 가이드

## 📋 개요

BlueCrab 프로젝트의 로컬 개발 환경을 설정하기 위한 상세 가이드입니다.

## 🔧 필수 소프트웨어

### Java Development Kit (JDK)
- **버전**: Java 11 이상
- **권장**: OpenJDK 11 또는 Oracle JDK 11

```bash
# Java 버전 확인
java -version
javac -version

# 환경변수 설정 (Windows)
set JAVA_HOME=C:\Program Files\Java\jdk-11.0.x
set PATH=%JAVA_HOME%\bin;%PATH%
```

### Apache Maven
- **버전**: Maven 3.6 이상
- **설치**: [Maven 공식 사이트](https://maven.apache.org/download.cgi)

```bash
# Maven 버전 확인
mvn -version

# 환경변수 설정 (Windows)
set M2_HOME=C:\apache-maven-x.x.x
set PATH=%M2_HOME%\bin;%PATH%
```

### IDE (통합 개발 환경)
#### IntelliJ IDEA (권장)
- **버전**: Community Edition 또는 Ultimate
- **필수 플러그인**:
  - Spring Boot
  - Lombok Plugin
  - Database Navigator

#### Eclipse
- **버전**: Eclipse IDE for Enterprise Java Developers
- **필수 플러그인**:
  - Spring Tools 4 (STS4)
  - Lombok

### 데이터베이스
- **MariaDB** 또는 **MySQL 8.0+**
- 또는 **Oracle Database** (운영 환경과 동일)

## 🗄️ 데이터베이스 설정

### MariaDB 설치 및 설정
```bash
# MariaDB 설치 (Windows - Chocolatey)
choco install mariadb

# MariaDB 서비스 시작
net start mysql

# 데이터베이스 생성
mysql -u root -p
CREATE DATABASE blue_crab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'bluecrab_user'@'localhost' IDENTIFIED BY 'bluecrab_password';
GRANT ALL PRIVILEGES ON blue_crab.* TO 'bluecrab_user'@'localhost';
FLUSH PRIVILEGES;
```

### 원격 DB 연결 (개발서버)
```properties
# application-dev.properties
spring.datasource.url=jdbc:mariadb://121.165.24.26:55511/blue_crab
spring.datasource.username=KDT_project  
spring.datasource.password=KDT_project
```

## 📦 프로젝트 설정

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd BlueCrab
```

### 2. Maven 의존성 설치
```bash
# 의존성 다운로드 및 컴파일
mvn clean install

# 테스트 제외하고 빌드
mvn clean install -DskipTests
```

### 3. IDE 설정

#### IntelliJ IDEA
1. **프로젝트 열기**: `File > Open` → `pom.xml` 선택
2. **Lombok 설정**: 
   - `File > Settings > Plugins` → Lombok 플러그인 설치
   - `Settings > Build > Compiler > Annotation Processors` → Enable annotation processing 체크
3. **코딩 스타일**: 
   - `Settings > Editor > Code Style > Java` → 4 spaces 설정

#### Eclipse
1. **프로젝트 Import**: `File > Import > Existing Maven Projects`
2. **Lombok 설치**: 
   - lombok.jar 다운로드 후 실행
   - Eclipse 경로 지정하여 설치
3. **프로젝트 설정**: `Right-click project > Maven > Reload Projects`

### 4. 환경 설정 파일

#### application-dev.properties
```properties
# Database Configuration  
spring.datasource.url=jdbc:mariadb://localhost:3306/blue_crab
spring.datasource.username=bluecrab_user
spring.datasource.password=bluecrab_password
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration (개발용)
app.jwt.secret=myDevelopmentSecretKey12345678901234567890123456789012345678901234567890
app.jwt.access-token-expiration=900000
app.jwt.refresh-token-expiration=86400000

# Logging Configuration
logging.level.BlueCrab.com.example=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
```

## 🚀 애플리케이션 실행

### 1. Maven을 통한 실행
```bash
# 개발 프로파일로 실행
mvn spring-boot:run -Dspring.profiles.active=dev

# 특정 포트로 실행
mvn spring-boot:run -Dspring.profiles.active=dev -Dserver.port=8080
```

### 2. IDE에서 실행
- **Main Class**: `BlueCrab.com.example.BlueCrabApplication`
- **VM Options**: `-Dspring.profiles.active=dev`
- **Program Arguments**: `--server.port=8080`

### 3. WAR 파일 실행
```bash
# WAR 파일 빌드
mvn clean package

# Tomcat에 배포
# target/BlueCrab-1.0.0.war를 tomcat/webapps/에 복사
```

## 🧪 개발 환경 검증

### Health Check
```bash
# 애플리케이션 실행 상태 확인
curl http://localhost:8080/actuator/health

# 응답 예시:
# {"status":"UP"}
```

### API 테스트
```bash
# 로그인 테스트
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@bluecrab.com",
    "password": "test123!"
  }'

# 토큰 검증
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔧 개발 도구 설정

### Postman 컬렉션
```json
{
  "info": {
    "name": "BlueCrab API Collection"
  },
  "auth": {
    "type": "bearer",
    "bearer": [{
      "key": "token",
      "value": "{{jwt_token}}"
    }]
  },
  "item": [
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/auth/login",
        "header": [{
          "key": "Content-Type",
          "value": "application/json"
        }],
        "body": {
          "raw": "{\n  \"username\": \"test@example.com\",\n  \"password\": \"password123\"\n}"
        }
      }
    }
  ]
}
```

### Git Hooks 설정
```bash
# pre-commit hook 설정
#!/bin/sh
# .git/hooks/pre-commit

echo "Running pre-commit checks..."

# 코드 포맷 검사
mvn spotless:check

# 단위 테스트 실행
mvn test

if [ $? -ne 0 ]; then
    echo "Pre-commit checks failed. Please fix the issues before committing."
    exit 1
fi

echo "Pre-commit checks passed!"
```

## 📊 모니터링 도구

### Spring Boot Actuator
```properties
# Actuator 엔드포인트 활성화
management.endpoints.web.exposure.include=health,info,metrics,env
management.endpoint.health.show-details=always
```

### 로그 설정
```xml
<!-- log4j2.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <File name="FileAppender" fileName="logs/application.log">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"/>
        </File>
    </Appenders>
    <Loggers>
        <Logger name="BlueCrab.com.example" level="DEBUG"/>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="FileAppender"/>
        </Root>
    </Loggers>
</Configuration>
```

## 🐛 문제 해결

### 자주 발생하는 문제들

#### 1. 포트 충돌
```bash
# 포트 사용 중인 프로세스 확인 (Windows)
netstat -ano | findstr :8080

# 프로세스 종료
taskkill /PID <PID> /F

# 다른 포트로 실행
mvn spring-boot:run -Dserver.port=8081
```

#### 2. 데이터베이스 연결 오류
```bash
# MariaDB 서비스 상태 확인
net start | findstr mysql

# 연결 테스트
mysql -h localhost -P 3306 -u bluecrab_user -p blue_crab
```

#### 3. Maven 의존성 문제
```bash
# 의존성 캐시 삭제
mvn dependency:purge-local-repository

# 강제 업데이트
mvn clean install -U
```

#### 4. Lombok 인식 안됨
- IDE에 Lombok 플러그인 설치 확인
- Annotation Processing 활성화 확인
- IDE 재시작 후 프로젝트 클린 빌드

### 성능 최적화

#### JVM 옵션 설정
```bash
# 개발 환경용 JVM 옵션
export JAVA_OPTS="-Xms512m -Xmx1024m -XX:MetaspaceSize=256m"

# 또는 IDE Run Configuration에서 설정
-Xms512m -Xmx1024m -XX:MetaspaceSize=256m
```

#### HikariCP 최적화
```properties
# 개발 환경용 커넥션 풀 설정
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

## 📝 개발 워크플로우

### 1. 개발 프로세스
1. **Feature Branch 생성**: `git checkout -b feature/기능명`
2. **코드 작성 및 테스트**: TDD 방식 권장
3. **코드 리뷰**: Pull Request 생성
4. **Merge**: 리뷰 완료 후 develop branch로 병합

### 2. 테스트 실행
```bash
# 전체 테스트 실행
mvn test

# 특정 테스트 클래스 실행  
mvn test -Dtest=AuthServiceTest

# 통합 테스트 실행
mvn integration-test
```

### 3. 코드 품질 검사
```bash
# Checkstyle 실행
mvn checkstyle:check

# SpotBugs 실행
mvn spotbugs:check

# 테스트 커버리지 확인
mvn jacoco:report
```

---

> 💡 **추가 도움이 필요하시면** 팀 내 선임 개발자나 DevOps 팀에 문의해주세요!
> 
> **빠른 시작**: 위의 모든 단계를 순서대로 따라하면 약 30분 내에 개발 환경 구축이 완료됩니다.