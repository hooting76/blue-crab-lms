# 기술 스택 및 버전 정보

> **폴더**: `tech-stack/`  
> **목적**: 실제 운영 환경의 기술 스택 및 라이브러리 버전 정보

---

## 📄 문서 목록

### 기술스택_및_버전정보.md

**SSH 접속으로 확인한 실제 운영 환경 정보**

#### 포함 내용

✅ **운영 환경**
- Java 21.0.7 LTS (Oracle JDK)
- Apache Tomcat 9.0.108 (외장 톰캣)
- Debian 12 (bookworm)

✅ **백엔드 프레임워크**
- Spring Boot 2.7.18
- Spring Framework 5.3.37
- Spring Security 5.8.13
- eGovFrame 4.3.0

✅ **데이터 & 캐시**
- Redis 7.0.15
- Lettuce 6.1.10.RELEASE
- Hibernate 5.6.15.Final
- MariaDB JDBC 3.1.4

✅ **외부 서비스**
- Firebase Admin SDK 9.7.0
- MinIO Client 8.5.2
- JWT (jjwt) 0.12.6

✅ **라이브러리**
- Jackson 2.13.5
- Apache HttpClient 5.2.1
- SLF4J 1.7.36
- Log4j 2.17.2

#### 업데이트 방법

```bash
# SSH 접속
ssh user@server

# Java 버전 확인
java -version

# Tomcat 버전 확인
$CATALINA_HOME/bin/version.sh

# Redis 버전 확인
redis-cli --version

# 배포된 라이브러리 확인
ls -la $CATALINA_HOME/webapps/BlueCrab-1.0.0/WEB-INF/lib/
```

---

## 🎯 사용 대상

- **프론트엔드 개발자**: 호환성 확인
- **백엔드 개발자**: 개발 환경 구성
- **DevOps**: 서버 구성 및 배포

---

## 📅 최종 업데이트

- **날짜**: 2025-10-10
- **확인 방법**: SSH 접속으로 실제 서버 확인
- **다음 업데이트**: 주요 버전 업그레이드 시
