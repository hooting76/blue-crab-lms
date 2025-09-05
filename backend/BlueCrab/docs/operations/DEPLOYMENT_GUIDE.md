# 🚀 BlueCrab 배포 가이드

## 📋 **배포 환경 개요**

### **운영 환경 정보**
- **서버**: https://bluecrab.chickenkiller.com
- **포트**: 8443 (HTTPS)
- **WAS**: Apache Tomcat 9.0.108
- **데이터베이스**: Oracle Database
- **Java**: OpenJDK 11

## 🔧 **배포 전 준비사항**

### **1. 빌드 환경 확인**
```bash
# Java 버전 확인
java -version

# Maven 버전 확인
mvn -version

# 프로젝트 빌드 테스트
mvn clean compile test
```

### **2. 설정 파일 확인**
```properties
# application.properties
spring.profiles.active=prod
server.port=8080

# application-prod.properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/XEPDB1
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

### **3. 환경 변수 설정**
```bash
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret_key
```

## 📦 **배포 프로세스**

### **1. 프로젝트 빌드**
```bash
# 프로젝트 루트에서 실행
cd BlueCrab
mvn clean package -Pprod
```

### **2. WAR 파일 생성 확인**
```bash
ls -la target/BlueCrab-1.0.0.war
```

### **3. 서버 배포**

#### **방법 1: SCP를 통한 파일 전송**
```bash
scp target/BlueCrab-1.0.0.war user@server:/path/to/tomcat/webapps/
```

#### **방법 2: 서버에서 직접 빌드**
```bash
# 서버에서 실행
git pull origin main
mvn clean package -Pprod
cp target/BlueCrab-1.0.0.war $TOMCAT_HOME/webapps/
```

### **4. Tomcat 재시작**
```bash
# Tomcat 중지
$TOMCAT_HOME/bin/shutdown.sh

# 기존 배포 파일 정리
rm -rf $TOMCAT_HOME/webapps/BlueCrab-1.0.0/
rm -f $TOMCAT_HOME/webapps/BlueCrab-1.0.0.war.original

# Tomcat 시작
$TOMCAT_HOME/bin/startup.sh
```

## 🔍 **배포 후 검증**

### **1. 애플리케이션 시작 확인**
```bash
# 로그 확인
tail -f $TOMCAT_HOME/logs/catalina.out

# 프로세스 확인
ps aux | grep tomcat
```

### **2. Health Check**
```bash
# 기본 연결 확인
curl -k https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/ping

# 응답 시간 확인
curl -k -w "@curl-format.txt" -o /dev/null -s \
  https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/ping
```

### **3. 기능 테스트**
```bash
# 로그인 테스트
curl -X POST https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@bluecrab.com","password":"test123!"}'

# API 응답 확인
curl -H "Authorization: Bearer <token>" \
  https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/users/stats
```

## 📊 **모니터링 설정**

### **1. 로그 모니터링**
```bash
# 애플리케이션 로그
tail -f $TOMCAT_HOME/logs/bluecrab-app.log

# 에러 로그
tail -f $TOMCAT_HOME/logs/bluecrab-error.log

# 인증 로그
tail -f $TOMCAT_HOME/logs/bluecrab-auth.log
```

### **2. 시스템 리소스 모니터링**
```bash
# CPU/메모리 사용량
top -p $(pgrep -f tomcat)

# 디스크 사용량
df -h

# 로그 파일 크기
du -sh $TOMCAT_HOME/logs/
```

### **3. 데이터베이스 연결 확인**
```sql
-- Oracle에서 연결 상태 확인
SELECT COUNT(*) FROM v$session WHERE program LIKE '%JDBC%';

-- 활성 쿼리 확인
SELECT sql_text, elapsed_time/1000000 as elapsed_seconds 
FROM v$sql 
WHERE elapsed_time > 1000000 
ORDER BY elapsed_time DESC;
```

## 🚨 **롤백 절차**

### **1. 빠른 롤백 (WAR 파일 교체)**
```bash
# 이전 버전으로 되돌리기
cp $BACKUP_DIR/BlueCrab-1.0.0.war.backup $TOMCAT_HOME/webapps/BlueCrab-1.0.0.war

# Tomcat 재시작
$TOMCAT_HOME/bin/shutdown.sh && sleep 5 && $TOMCAT_HOME/bin/startup.sh
```

### **2. 데이터베이스 롤백 (필요시)**
```sql
-- 백업에서 복원
-- (사전에 준비된 백업 스크립트 실행)
```

## 🔐 **보안 체크리스트**

### **배포 전 확인사항**
- [ ] 민감한 정보가 소스코드에 하드코딩되지 않았는가?
- [ ] 환경변수로 설정 정보를 관리하고 있는가?
- [ ] HTTPS 설정이 올바른가?
- [ ] JWT 시크릿 키가 안전하게 관리되고 있는가?
- [ ] 데이터베이스 접근 권한이 최소한으로 설정되었는가?

### **배포 후 확인사항**
- [ ] 로그인/로그아웃이 정상 동작하는가?
- [ ] API 응답 시간이 acceptable한가?
- [ ] 에러 로그에 예상치 못한 오류가 없는가?
- [ ] 리소스 사용량이 정상 범위인가?

## 📝 **배포 체크리스트**

```
배포 전:
□ 소스코드 최신화 (git pull)
□ 테스트 실행 (mvn test)
□ 빌드 성공 확인 (mvn package)
□ 설정 파일 검증
□ 백업 생성

배포 중:
□ WAR 파일 업로드
□ Tomcat 재시작
□ 로그 모니터링

배포 후:
□ Health Check 수행
□ 기능 테스트 실행
□ 성능 모니터링
□ 에러 로그 확인
□ 사용자 알림 (필요시)
```

## 🔧 **트러블슈팅**

### **일반적인 문제와 해결책**

#### **1. OutOfMemoryError**
```bash
# Tomcat JVM 옵션 조정
export CATALINA_OPTS="-Xms512m -Xmx2048m -XX:MetaspaceSize=256m"
```

#### **2. 데이터베이스 연결 실패**
```bash
# 연결 설정 확인
grep -r "datasource" application*.properties

# 네트워크 연결 확인
telnet database_host 1521
```

#### **3. 로그 파일 가득참**
```bash
# 로그 파일 압축/삭제
find $TOMCAT_HOME/logs -name "*.log" -mtime +7 -exec gzip {} \;
find $TOMCAT_HOME/logs -name "*.gz" -mtime +30 -delete
```

---

**작성일**: 2025-08-27  
**버전**: 1.0.0
