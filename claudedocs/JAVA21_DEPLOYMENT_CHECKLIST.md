# Java 21 배포 환경 검증 체크리스트

## 개요
이 문서는 Blue Crab LMS 백엔드를 Java 21로 업그레이드한 후, 배포 환경에서 확인해야 할 사항들을 정리한 체크리스트입니다.

---

## 1. 서버 환경 검증

### 1.1 Java 21 설치 확인

#### Debian/Ubuntu 서버
```bash
# Java 버전 확인
java -version

# 예상 출력:
# openjdk version "21.0.8" 2025-04-15 LTS
# OpenJDK Runtime Environment (build 21.0.8+7-Ubuntu-...)
# OpenJDK 64-Bit Server VM (build 21.0.8+7-Ubuntu-..., mixed mode, sharing)
```

#### Java 21 설치 (필요시)
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk -y

# 설치 확인
java -version
javac -version

# JAVA_HOME 환경 변수 설정
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 1.2 시스템 PATH 확인
```bash
# 현재 Java 경로 확인
which java
# 출력: /usr/bin/java

# 실제 Java 경로 확인 (심볼릭 링크 추적)
readlink -f $(which java)
# 출력: /usr/lib/jvm/java-21-openjdk-amd64/bin/java

# JAVA_HOME 확인
echo $JAVA_HOME
# 출력: /usr/lib/jvm/java-21-openjdk-amd64
```

---

## 2. 빌드 환경 검증

### 2.1 Maven 버전 확인
```bash
# Maven 버전 확인
mvn -version

# 예상 출력:
# Apache Maven 3.8.7
# Maven home: /usr/share/maven
# Java version: 21.0.8, vendor: Ubuntu, runtime: /usr/lib/jvm/java-21-openjdk-amd64
```

### 2.2 프로젝트 빌드 테스트
```bash
cd /path/to/blue-crab-lms/backend/BlueCrab

# 클린 빌드 (테스트 포함)
mvn clean package

# 빌드 성공 확인
# [INFO] BUILD SUCCESS
# [INFO] Total time: 8-10 seconds
```

### 2.3 컴파일된 바이트코드 검증
```bash
# Java 21 바이트코드 버전 확인 (0x41 = 65 = Java 21)
xxd -l 8 target/classes/BlueCrab/com/example/BlueCrabApplication.class | head -n1
# 예상 출력: 00000000: cafe babe 0000 0041
```

---

## 3. Apache Tomcat 호환성 검증

### 3.1 Tomcat 버전 확인
```bash
# Tomcat 버전 확인
/opt/tomcat/bin/version.sh

# Java 21 호환 확인:
# - Tomcat 9.0.108: ✅ Java 21 호환 (최소 9.0.40+)
# - Tomcat 10.x: ✅ Java 21 호환
# - Tomcat 8.5.x: ⚠️ Java 11까지만 지원
```

### 3.2 Tomcat 환경 변수 설정
```bash
# /opt/tomcat/bin/setenv.sh 파일 생성/수정
sudo nano /opt/tomcat/bin/setenv.sh
```

#### setenv.sh 내용:
```bash
#!/bin/bash

# Java 21 설정
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export JRE_HOME=$JAVA_HOME

# JVM 옵션 설정
export CATALINA_OPTS="$CATALINA_OPTS -Xms512m"
export CATALINA_OPTS="$CATALINA_OPTS -Xmx2048m"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxGCPauseMillis=200"

# Java 21 특화 최적화 (선택사항)
# Virtual Threads 활성화 (Spring Boot 3.2+ 필요)
# export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseVirtualThreads"

# 로그 설정
export CATALINA_OPTS="$CATALINA_OPTS -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
```

#### 실행 권한 부여
```bash
sudo chmod +x /opt/tomcat/bin/setenv.sh
```

---

## 4. WAR 배포 검증

### 4.1 WAR 파일 배포
```bash
# WAR 파일 복사
sudo cp target/BlueCrab-1.0.0.war /opt/tomcat/webapps/BlueCrab.war

# 배포 확인 (자동 압축 해제)
ls -la /opt/tomcat/webapps/
# BlueCrab/  (디렉토리 생성 확인)
# BlueCrab.war
```

### 4.2 Tomcat 재시작
```bash
# systemd 사용 시
sudo systemctl restart tomcat

# 직접 실행 시
sudo /opt/tomcat/bin/shutdown.sh
sudo /opt/tomcat/bin/startup.sh
```

### 4.3 애플리케이션 시작 로그 확인
```bash
# 실시간 로그 모니터링
tail -f /opt/tomcat/logs/catalina.out

# 확인할 주요 로그:
# - Spring Boot 시작 메시지
# - Java 버전 정보
# - 포트 8080 리스닝 메시지
# - 에러 메시지 없음
```

---

## 5. 런타임 검증

### 5.1 헬스 체크
```bash
# Ping 엔드포인트 테스트
curl http://localhost:8080/BlueCrab/api/ping

# 예상 응답:
# {"success":true,"message":"pong","data":null,"timestamp":"..."}
```

### 5.2 Java 버전 확인 API (선택사항)
```bash
# JVM 정보 확인 (메트릭 엔드포인트 있을 경우)
curl http://localhost:8080/BlueCrab/api/metrics | jq '.data.jvm.version'
```

### 5.3 메모리 및 성능 모니터링
```bash
# JVM 프로세스 확인
jps -lv | grep BlueCrab

# 메모리 사용량 확인
ps aux | grep tomcat

# GC 로그 확인 (GC 로그 활성화 시)
tail -f /opt/tomcat/logs/gc.log
```

---

## 6. 빌드 최적화 도구 설치 (선택사항)

### 6.1 Maven Daemon (mvnd) 설치
빌드 속도를 78% 향상시키는 도구 (개발 환경용)

```bash
# 다운로드
cd ~
wget https://github.com/apache/maven-mvnd/releases/download/1.0.3/maven-mvnd-1.0.3-linux-amd64.tar.gz

# 압축 해제
tar -xzf maven-mvnd-1.0.3-linux-amd64.tar.gz

# PATH 추가
echo 'export PATH="$HOME/maven-mvnd-1.0.3-linux-amd64/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc

# 설치 확인
mvnd --version
```

### 6.2 사용법
```bash
# 기존 mvn 명령어를 mvnd로 대체
mvnd clean package -DskipTests

# 첫 빌드: ~8초
# 이후 빌드: ~4.4초 (46% 향상)
```

---

## 7. 환경별 체크리스트

### 7.1 개발 환경 (Development)
- [ ] Java 21 JDK 설치 확인
- [ ] Maven 3.6+ 설치 확인
- [ ] IDE Java 21 프로젝트 설정 확인
- [ ] `application-dev.properties` 설정 확인
- [ ] 로컬 빌드 성공 (`mvn clean package`)
- [ ] 로컬 실행 테스트 (`mvn spring-boot:run`)
- [ ] mvnd 설치 (선택사항)

### 7.2 운영 환경 (Production - Debian 12)
- [ ] Java 21 JRE 설치 확인
- [ ] JAVA_HOME 환경 변수 설정
- [ ] Tomcat 9.0.108+ 버전 확인
- [ ] Tomcat setenv.sh 설정
- [ ] WAR 파일 배포 성공
- [ ] Tomcat 재시작 및 로그 확인
- [ ] 헬스 체크 API 응답 확인
- [ ] 메모리 사용량 모니터링
- [ ] 방화벽 포트 8080 오픈 확인

---

## 8. 문제 해결 (Troubleshooting)

### 8.1 "Unsupported class file major version" 에러
```bash
# 원인: Tomcat이 Java 21보다 낮은 버전으로 실행 중
# 해결:
sudo nano /opt/tomcat/bin/setenv.sh
# JAVA_HOME을 Java 21 경로로 변경
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### 8.2 빌드 실패 - "target release 21 requires compiler compliance level 21"
```bash
# 원인: Maven이 Java 21보다 낮은 버전 사용
# 해결:
mvn -version  # Java 버전 확인
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn clean package  # 재빌드
```

### 8.3 Tomcat 시작 실패
```bash
# 로그 확인
tail -f /opt/tomcat/logs/catalina.out

# 일반적인 원인:
# - 포트 충돌 (8080 이미 사용 중)
# - 메모리 부족
# - 잘못된 WAR 파일 경로
```

---

## 9. 성능 비교 (참고)

### 9.1 빌드 성능
| 환경 | 빌드 도구 | 시간 | 개선율 |
|------|-----------|------|--------|
| Windows + Java 8 | mvn | ~20s | baseline |
| Ubuntu + Java 21 | mvn | 8.09s | 60% |
| Ubuntu + Java 21 | mvnd | 4.36s | 78% |

### 9.2 런타임 예상 성능 향상
- JIT 컴파일 최적화: ~10-15%
- G1GC 개선: 낮은 지연 시간
- Virtual Threads: 동시성 처리 개선 (Spring Boot 3.2+ 필요)

---

## 10. 참고 문서
- [JAVA_UPGRADE_SUMMARY.md](./JAVA_UPGRADE_SUMMARY.md) - Java 21 업그레이드 전체 내역
- [pom.xml](../backend/BlueCrab/pom.xml) - Maven 빌드 설정
- [Apache Tomcat 9 Documentation](https://tomcat.apache.org/tomcat-9.0-doc/)
- [OpenJDK 21 Documentation](https://openjdk.org/projects/jdk/21/)

---

## 체크리스트 완료 확인

모든 항목을 확인한 후 아래 항목을 체크하세요:

- [ ] Java 21 설치 및 PATH 설정 완료
- [ ] Maven 빌드 성공
- [ ] Tomcat 호환성 확인 및 설정 완료
- [ ] WAR 배포 및 애플리케이션 시작 성공
- [ ] 헬스 체크 API 정상 응답
- [ ] 운영 환경 모니터링 설정 완료

---

**작성일**: 2025-10-31
**버전**: 1.0
**대상 환경**: Debian 12, Tomcat 9.0.108, Java 21
