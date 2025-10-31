# Blue Crab LMS - 빌드 가이드

## 빠른 시작

### 사전 요구사항
- Java 21 이상
- Maven 3.8.7 이상

### 기본 빌드 명령어

```bash
# 기본 빌드 (8초)
mvn clean package -DskipTests

# 테스트 포함 빌드
mvn clean package

# 컴파일만 (빠른 문법 체크)
mvn compile

# 테스트만 실행
mvn test
```

### 출력 위치

```
빌드 산출물 생성 위치:
target/BlueCrab-1.0.0.war    (130MB) ← 이 파일을 배포
```

## 최적화된 빌드 (개발용 권장)

### Maven Daemon (mvnd) 설치 - 일회성 설정

```bash
# 다운로드 및 설치
wget https://github.com/apache/maven-mvnd/releases/download/1.0.3/maven-mvnd-1.0.3-linux-amd64.tar.gz
tar -xzf maven-mvnd-1.0.3-linux-amd64.tar.gz

# PATH에 추가
echo 'export PATH="$HOME/maven-mvnd-1.0.3-linux-amd64/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc

# 선택사항: 별칭 생성
echo 'alias mb="mvnd clean package -DskipTests"' >> ~/.bashrc
echo 'alias mbt="mvnd clean package"' >> ~/.bashrc
source ~/.bashrc
```

### 더 빠른 빌드를 위한 mvnd 사용

```bash
# 첫 빌드: ~8초 (데몬 시작)
mvnd clean package -DskipTests

# 이후 빌드: ~4.4초 (46% 빠름!)
mvnd clean package -DskipTests

# 또는 별칭 사용
mb
```

## 빌드 성능

| 방법 | 시간 | 비고 |
|------|------|------|
| `mvn` | 8.09초 | 기본 Maven |
| `mvnd` (첫 실행) | 8.55초 | 데몬 시작 오버헤드 |
| `mvnd` (워밍업 완료) | 4.36초 | **권장** |

**일일 시간 절감**: 개발자당 ~1-2분

## 배포

### Tomcat에 배포

```bash
# Tomcat에 WAR 복사
cp target/BlueCrab-1.0.0.war /path/to/tomcat/webapps/

# 또는 루트 컨텍스트를 위해 ROOT.war로 이름 변경
cp target/BlueCrab-1.0.0.war /path/to/tomcat/webapps/ROOT.war
```

### 내장 서버로 실행

```bash
java -jar target/BlueCrab-1.0.0.war
```

## 전자정부프레임워크 호환성

이 프로젝트는 **전자정부프레임워크 Boot 4.3.0** (Spring Boot 기반)을 사용합니다:
- 빌드를 위한 특수 IDE 불필요
- 표준 Maven/Gradle 프로젝트 구조
- 어떤 IDE나 명령줄로도 빌드 가능
- `pom.xml`을 통해 전자정부프레임워크 라이브러리 자동 포함

## 빌드 설정

### Java 버전

```xml
<!-- pom.xml -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

### 테스트 건너뛰기

```bash
# Maven
mvn clean package -DskipTests

# Maven Daemon
mvnd clean package -DskipTests
```

### 오프라인 모드

```bash
# 의존성 한 번만 다운로드
mvn dependency:go-offline

# 업데이트 확인 없이 빌드
mvn clean package -o
```

## 문제 해결

### 빌드 실패 - 메모리 부족

```bash
# Maven 메모리 증가
export MAVEN_OPTS="-Xmx2g"
mvn clean package
```

### 빌드 실패 - 잘못된 Java 버전

```bash
# Java 버전 확인
java -version  # Java 21이 표시되어야 함

# JAVA_HOME 설정
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### mvnd 데몬 문제

```bash
# 데몬 중지 후 재시도
mvnd --stop
mvnd clean package -DskipTests

# 데몬 상태 확인
mvnd --status
```

## CI/CD

자동화 빌드(Jenkins, GitHub Actions, GitLab CI)의 경우 기본 Maven 사용:

```yaml
# .gitlab-ci.yml 예시
build:
  stage: build
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.war
```

**참고**: mvnd는 반복적인 로컬 빌드에 최적화되어 있으며, CI/CD에는 적합하지 않습니다.

## 추가 정보

- [상세 빌드 최적화 가이드](../../BUILD_OPTIMIZATION_GUIDE.ko.md)
- [Java 21 업그레이드 요약](../../JAVA_UPGRADE_SUMMARY.ko.md)
- [프로젝트 README](../../README.md)

## 시스템 요구사항

**개발 환경:**
- OS: Linux/Ubuntu (권장), macOS, Windows
- Java: 21 이상 (LTS)
- Maven: 3.8.7 이상
- RAM: 최소 4GB, 권장 8GB
- 디스크: 의존성을 위한 500MB

**프로덕션 환경:**
- Java 런타임: 21 이상
- 애플리케이션 서버: Tomcat 10+ 또는 Jakarta EE 9+ 호환 서버
- RAM: 최소 2GB, 권장 4GB

## 빌드 명령어 요약

```bash
# 개발 중 (가장 자주 사용)
mvnd clean package -DskipTests        # 또는 mb

# 테스트 포함 (커밋 전)
mvnd clean package                    # 또는 mbt

# 컴파일만 (빠른 확인)
mvnd compile                          # 또는 mc

# 테스트만
mvnd test                             # 또는 mt

# 클린
mvnd clean                            # 또는 mclean

# 로컬 설치
mvnd install                          # 또는 mi
```

## Maven vs Maven Daemon 비교

```
mvn (기본 Maven):
- 빌드 시간: 8.09초
- 매번 JVM 시작
- 설치 불필요
- CI/CD에 적합

mvnd (Maven Daemon):
- 첫 빌드: 8.55초
- 이후 빌드: 4.36초
- JVM이 백그라운드에 유지
- 추가 설치 필요
- 개발용으로 최적
```

## 컴파일 vs 빌드 차이

```
mvn compile:
- 소스 코드만 컴파일
- .class 파일 생성
- WAR 파일 생성 안 됨
- 빠름 (~5초)
- 용도: 문법 체크

mvn package (빌드):
- 컴파일 + 테스트 + 패키징
- WAR 파일 생성
- 배포 가능
- 느림 (~8초)
- 용도: 실제 배포
```

## 빌드 산출물

```
target/
├── BlueCrab-1.0.0.war          ← 최종 배포 파일 (130MB)
├── BlueCrab-1.0.0/             ← WAR 압축 해제 버전
│   ├── META-INF/
│   └── WEB-INF/
│       ├── classes/            ← 컴파일된 클래스
│       └── lib/                ← 의존성 라이브러리
├── classes/                    ← 소스 컴파일 결과
├── test-classes/               ← 테스트 컴파일 결과
└── surefire-reports/          ← 테스트 리포트
```

## 자주 묻는 질문

**Q: 전자정부프레임워크 IDE(이클립스)가 필요한가요?**
A: 아니요. 이 프로젝트는 표준 Maven 프로젝트입니다. 어떤 IDE나 명령줄에서도 빌드할 수 있습니다.

**Q: 왜 mvnd가 더 빠른가요?**
A: JVM을 백그라운드에 유지하여 시작 오버헤드를 제거하고, 클래스와 플러그인을 캐시합니다.

**Q: CI/CD에서 mvnd를 사용해야 하나요?**
A: 아니요. CI/CD는 일회성 빌드이므로 기본 Maven이 적합합니다.

**Q: 병렬 빌드 (-T 옵션)를 사용해야 하나요?**
A: 아니요. Blue Crab은 단일 모듈 프로젝트이므로 효과가 없습니다.

**Q: 빌드 시간이 느려졌어요?**
A: `mvnd --stop`으로 데몬을 재시작하거나 `mvn clean`으로 캐시를 정리하세요.

---

**최종 업데이트**: 2025-10-31
**문서 버전**: 1.0
