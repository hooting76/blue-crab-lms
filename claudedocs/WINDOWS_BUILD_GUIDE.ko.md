# Blue Crab LMS - Windows 빌드 최적화 가이드

## 개요

Windows 환경에서도 Java 21 업그레이드와 Maven Daemon을 통해 빌드 성능을 **50% 향상**시킬 수 있습니다.
Linux만큼 극적이지는 않지만, 충분한 개선 효과가 있습니다.

## Windows vs Linux 성능 비교

### 예상 빌드 시간 (같은 하드웨어 기준)

```
┌─────────────────────────────────────┬──────────┬────────────┐
│ 환경 설정                            │ 빌드시간 │ 개선율     │
├─────────────────────────────────────┼──────────┼────────────┤
│ Windows + Java 8 + Maven            │ ~20초    │ 기준       │
│ Windows + Java 21 + Maven           │ ~15초    │ 25% ✓      │
│ Windows + Java 21 + Maven Daemon    │ ~10초    │ 50% ✓✓     │
│                                      │          │            │
│ Linux + Java 8 + Maven              │ ~12초    │ 40%        │
│ Linux + Java 21 + Maven             │ ~8초     │ 60%        │
│ Linux + Java 21 + Maven Daemon      │ ~4.4초   │ 78% 🔥     │
└─────────────────────────────────────┴──────────┴────────────┘
```

### 왜 Linux가 더 빠른가?

**Windows의 구조적 제약:**

1. **파일 시스템 (NTFS)**
   - 작은 파일 다수 처리 시 느림
   - Maven은 수천 개의 `.class` 파일 생성
   - 메타데이터 오버헤드가 큼

2. **Windows Defender**
   - 새로 생성된 파일마다 실시간 스캔
   - 빌드 중 수천 개 파일 검사
   - 일반적으로 20-40% 시간 추가

3. **프로세스 생성 비용**
   - Windows의 프로세스 생성은 무거움
   - Maven은 여러 javac 프로세스 실행
   - Linux fork()보다 3-5배 느림

4. **I/O 스케줄링**
   - GUI 애플리케이션에 우선순위
   - 백그라운드 빌드는 낮은 우선순위

**하지만 여전히 개선은 가능합니다!**

## Windows에서 Java 21 업그레이드

### 1. Java 21 설치

#### Oracle JDK 21 (상용, 라이선스 필요)

```powershell
# 다운로드
https://www.oracle.com/java/technologies/downloads/#java21

# 설치 후 환경변수 설정
setx JAVA_HOME "C:\Program Files\Java\jdk-21"
setx PATH "%PATH%;%JAVA_HOME%\bin"

# 확인
java -version
```

#### OpenJDK 21 (무료, 권장)

```powershell
# Microsoft Build of OpenJDK (권장)
winget install Microsoft.OpenJDK.21

# 또는 Adoptium Eclipse Temurin
https://adoptium.net/temurin/releases/?version=21

# 확인
java -version
# 출력: openjdk version "21.0.x"
```

#### 환경변수 설정 (수동)

```
1. 시스템 속성 → 환경 변수
2. 시스템 변수:
   - JAVA_HOME = C:\Program Files\Microsoft\jdk-21.x.x
   - Path에 %JAVA_HOME%\bin 추가
3. 명령 프롬프트 재시작
4. 확인: java -version
```

### 2. pom.xml 업데이트

Blue Crab LMS 프로젝트의 `pom.xml`은 이미 Java 21 설정이 되어 있습니다:

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

### 3. Maven 설치 확인

```powershell
# Maven 버전 확인
mvn -version

# Maven이 Java 21을 사용하는지 확인
# 출력에 "Java version: 21.x.x"가 표시되어야 함
```

### 4. 첫 빌드 테스트

```powershell
cd backend\BlueCrab

# 빌드 시간 측정
Measure-Command { mvn clean package -DskipTests }

# 예상: ~15초 (Java 8에서는 ~20초)
```

## Maven Daemon (mvnd) 설치 - Windows

### 설치 방법

#### 방법 1: Chocolatey (권장)

```powershell
# Chocolatey 설치 (없는 경우)
Set-ExecutionPolicy Bypass -Scope Process -Force
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# mvnd 설치
choco install mvnd

# 확인
mvnd --version
```

#### 방법 2: 수동 설치

```powershell
# 다운로드
https://github.com/apache/maven-mvnd/releases/download/1.0.3/maven-mvnd-1.0.3-windows-amd64.zip

# 압축 해제 (예: C:\tools\maven-mvnd)

# 환경변수 Path에 추가
setx PATH "%PATH%;C:\tools\maven-mvnd-1.0.3-windows-amd64\bin"

# 명령 프롬프트 재시작 후 확인
mvnd --version
```

### mvnd 사용

```powershell
cd backend\BlueCrab

# 첫 빌드 (데몬 시작)
Measure-Command { mvnd clean package -DskipTests }
# 예상: ~15초

# 두 번째 빌드 (데몬 재사용)
Measure-Command { mvnd clean package -DskipTests }
# 예상: ~10초 (33% 빠름!)
```

## Windows 전용 최적화

### 1. Windows Defender 제외 설정

Maven 빌드가 느린 가장 큰 이유는 실시간 스캔입니다.

```
Windows 보안 → 바이러스 및 위협 방지 → 설정 관리
→ 제외 항목 추가

다음 폴더를 제외 목록에 추가:
├─ C:\Users\[사용자명]\.m2\repository
├─ [프로젝트경로]\target
└─ C:\Program Files\Java\jdk-21

⚠️ 보안 경고:
- 신뢰할 수 있는 프로젝트만 제외
- 회사 보안 정책 확인 필요
```

**효과:** 20-30% 빌드 시간 단축

### 2. SSD 사용 (HDD라면)

```
HDD (5400rpm): 빌드 ~30초
SSD (SATA):    빌드 ~15초
NVMe SSD:      빌드 ~10초

→ 가능하면 프로젝트를 SSD에 위치
```

### 3. RAM 디스크 사용 (고급)

```powershell
# ImDisk Toolkit 사용 (무료)
https://sourceforge.net/projects/imdisk-toolkit/

# 4GB RAM 디스크 생성 (R: 드라이브)
# Maven 로컬 저장소를 RAM 디스크로 설정

# settings.xml
<localRepository>R:\.m2\repository</localRepository>

# 효과: 10-20% 추가 향상
# 주의: 시스템 재시작 시 데이터 손실
```

### 4. PowerShell 대신 Command Prompt 사용

```
PowerShell: 스크립트 보안 검사로 약간 느림
Command Prompt: 더 빠름

빌드 시에는 cmd.exe 사용 권장
```

### 5. JVM 메모리 최적화

```powershell
# Maven 메모리 증가
setx MAVEN_OPTS "-Xmx4g -XX:+UseG1GC"

# 또는 프로젝트별 설정
# .mvn\jvm.config 파일 생성
-Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

## Windows에서 기대 효과

### 최적화 단계별 효과

```
기준: Windows + Java 8 + Maven = 20초

1단계: Java 21 업그레이드
└─ Windows + Java 21 + Maven = 15초 (25% 향상)

2단계: Maven Daemon 사용
└─ Windows + Java 21 + mvnd = 10초 (50% 향상)

3단계: Defender 제외 설정
└─ Windows + Java 21 + mvnd + 제외 = 8초 (60% 향상)

4단계: SSD 사용 (HDD에서)
└─ Windows + Java 21 + mvnd + 제외 + SSD = 7초 (65% 향상)
```

### 실무 시간 절감 (Windows 기준)

```
Java 8 + mvn (20초):
20회 빌드 × 20초 = 400초 (6분 40초)

Java 21 + mvnd (10초):
1회 × 15초 + 19회 × 10초 = 205초 (3분 25초)

일일 절감: 195초 (3분 15초)
주간 절감: 975초 (16분)
월간 절감: 3,900초 (65분)
```

## Windows 빌드 스크립트

### PowerShell 스크립트

`build.ps1` 생성:

```powershell
# Blue Crab LMS 빠른 빌드 스크립트

param(
    [switch]$SkipTests = $true,
    [switch]$Clean = $true
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Blue Crab LMS 빌드 시작..." -ForegroundColor Cyan

$startTime = Get-Date

try {
    Set-Location "backend\BlueCrab"

    if ($Clean) {
        Write-Host "🧹 Clean..." -ForegroundColor Yellow
        mvnd clean -q
    }

    Write-Host "⚙️  빌드 중..." -ForegroundColor Yellow

    if ($SkipTests) {
        mvnd package -DskipTests -q
    } else {
        mvnd package -q
    }

    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalSeconds

    Write-Host "✅ 빌드 성공! ($duration 초)" -ForegroundColor Green
    Write-Host "📦 산출물: target\BlueCrab-1.0.0.war" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ 빌드 실패!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
```

사용법:
```powershell
# 기본 빌드 (테스트 제외)
.\build.ps1

# 테스트 포함
.\build.ps1 -SkipTests:$false

# Clean 없이
.\build.ps1 -Clean:$false
```

### Batch 스크립트

`build.bat` 생성:

```batch
@echo off
echo 🚀 Blue Crab LMS 빌드 시작...

cd backend\BlueCrab

set START_TIME=%TIME%

mvnd clean package -DskipTests -q

if %ERRORLEVEL% EQU 0 (
    echo ✅ 빌드 성공!
    echo 📦 산출물: target\BlueCrab-1.0.0.war
) else (
    echo ❌ 빌드 실패!
    exit /b 1
)

set END_TIME=%TIME%
echo ⏱️  시작: %START_TIME%
echo ⏱️  종료: %END_TIME%
```

사용법:
```cmd
build.bat
```

## IDE 설정 (Windows)

### IntelliJ IDEA

```
File → Settings → Build, Execution, Deployment → Build Tools → Maven

Maven 설정:
├─ Maven home path: C:\tools\maven-mvnd-1.0.3-windows-amd64\mvn
├─ User settings file: C:\Users\[사용자명]\.m2\settings.xml
└─ Local repository: C:\Users\[사용자명]\.m2\repository

JVM Options:
-Xmx4g -XX:+UseG1GC
```

### Eclipse

```
Window → Preferences → Maven → Installations

Add... → C:\tools\maven-mvnd-1.0.3-windows-amd64\mvn
Set as default
```

### VS Code

```json
// settings.json
{
  "maven.executable.path": "C:\\tools\\maven-mvnd-1.0.3-windows-amd64\\bin\\mvnd.cmd",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "C:\\Program Files\\Microsoft\\jdk-21.0.x",
      "default": true
    }
  ]
}
```

## Windows 문제 해결

### mvnd가 시작되지 않음

```powershell
# Java 버전 확인
java -version

# JAVA_HOME 확인
echo %JAVA_HOME%

# 수동으로 JAVA_HOME 지정
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.x"
mvnd clean package
```

### "Long Path" 오류

Windows 경로 길이 제한 (260자) 문제:

```powershell
# 관리자 권한 PowerShell
New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" `
  -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force

# 재부팅 필요
```

### 빌드 중 메모리 부족

```powershell
# Maven 메모리 증가
setx MAVEN_OPTS "-Xmx4g"

# mvnd 메모리 설정
# C:\Users\[사용자명]\.m2\mvnd.properties
mvnd.maxHeapSize=4g
mvnd.minHeapSize=1g
```

### 한글 경로 문제

```
문제: C:\사용자\홍길동\프로젝트\...
해결: 영문 경로 사용 권장
     C:\Users\username\projects\...
```

## WSL2 사용 (최고 성능)

Windows에서 Linux 성능을 얻는 방법:

### WSL2 설치

```powershell
# 관리자 권한 PowerShell
wsl --install

# 재부팅 후
wsl --install -d Ubuntu-24.04
```

### WSL2에서 개발

```bash
# WSL Ubuntu에서
cd /mnt/c/projects/blue-crab-lms

# Java 21 설치
sudo apt update
sudo apt install openjdk-21-jdk

# Maven Daemon 설치
wget https://github.com/apache/maven-mvnd/releases/download/1.0.3/maven-mvnd-1.0.3-linux-amd64.tar.gz
tar -xzf maven-mvnd-1.0.3-linux-amd64.tar.gz
export PATH="$HOME/maven-mvnd-1.0.3-linux-amd64/bin:$PATH"

# 빌드
mvnd clean package -DskipTests
# → ~4.4초! (Linux 성능)
```

**WSL2 장점:**
- Linux 성능 (4.4초)
- Windows에서 파일 접근 가능
- IDE (IntelliJ, VS Code)도 연동 가능

**WSL2 단점:**
- 초기 설정 필요
- `/mnt/c` 경로는 약간 느림 (WSL 내부 경로 사용 권장)

## 결론

### Windows에서 Java 21 업그레이드 효과

```
✅ 가능: 25-50% 성능 향상
✅ 권장: Java 21 + Maven Daemon + Defender 제외
❌ 한계: Linux만큼 빠르지는 않음 (OS 제약)

최적화 전: 20초
최적화 후: 8-10초 (50-60% 향상)

vs Linux: 4.4초 (여전히 2배 빠름)
```

### 권장 사항

1. **반드시 하세요:**
   - Java 21 업그레이드
   - Maven Daemon 사용

2. **가능하면 하세요:**
   - Windows Defender 제외 설정
   - SSD 사용

3. **최고 성능 원한다면:**
   - WSL2 사용 (Linux 성능)

---

**최종 업데이트**: 2025-10-31
**대상 OS**: Windows 10/11
**테스트 환경**: Windows 11, Java 21, mvnd 1.0.3
