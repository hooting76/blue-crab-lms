# BlueCrab 로그 관리 가이드 (외장 톰캣 환경)

## 📁 로그 파일 위치

### 1. 톰캣 환경에서의 로그 경로
```bash
# 일반적인 로그 위치
$CATALINA_BASE/logs/bluecrab-*.log

# 예시: CentOS/RHEL 환경
/opt/tomcat/logs/bluecrab-app.log
/opt/tomcat/logs/bluecrab-auth.log
/opt/tomcat/logs/bluecrab-error.log

# Ubuntu 환경
/var/lib/tomcat9/logs/bluecrab-*.log
```

## 🔍 SSH에서 로그 확인 명령어

### 실시간 로그 모니터링
```bash
# 실시간 애플리케이션 로그 확인
tail -f $CATALINA_BASE/logs/bluecrab-app.log

# 실시간 인증 로그 확인
tail -f $CATALINA_BASE/logs/bluecrab-auth.log

# 실시간 에러 로그 확인
tail -f $CATALINA_BASE/logs/bluecrab-error.log

# 모든 로그 동시 모니터링
tail -f $CATALINA_BASE/logs/bluecrab-*.log
```

### 로그 검색 및 분석
```bash
# 특정 날짜 로그 검색 (오늘)
grep "$(date '+%Y-%m-%d')" $CATALINA_BASE/logs/bluecrab-app.log

# 에러 로그만 검색
grep "ERROR" $CATALINA_BASE/logs/bluecrab-app.log

# 특정 사용자 로그인 시도 검색
grep "Login attempt" $CATALINA_BASE/logs/bluecrab-auth.log | grep "username=testuser"

# 요청 ID로 관련 로그 추적
grep "ABC12345" $CATALINA_BASE/logs/bluecrab-*.log

# 최근 1시간 로그 확인
find $CATALINA_BASE/logs -name "bluecrab-*.log" -newermt "1 hour ago" -exec tail -100 {} \;
```

### 로그 파일 관리
```bash
# 로그 파일 크기 확인
ls -lh $CATALINA_BASE/logs/bluecrab-*.log

# 디스크 사용량 확인
du -sh $CATALINA_BASE/logs/

# 오래된 로그 파일 정리 (30일 이상)
find $CATALINA_BASE/logs -name "bluecrab-*.log.gz" -mtime +30 -delete

# 로그 파일 압축
gzip $CATALINA_BASE/logs/bluecrab-*.log.$(date -d yesterday '+%Y-%m-%d')
```

## 🚀 자동화 스크립트

### 로그 모니터링 스크립트 생성
```bash
# /home/admin/bluecrab-monitor.sh 생성
cat > /home/admin/bluecrab-monitor.sh << 'EOF'
#!/bin/bash

CATALINA_BASE=${CATALINA_BASE:-/opt/tomcat}
LOG_DIR="$CATALINA_BASE/logs"

echo "=== BlueCrab 로그 모니터링 ==="
echo "로그 디렉토리: $LOG_DIR"
echo "현재 시간: $(date)"
echo

# 로그 파일 상태 확인
echo "📋 로그 파일 상태:"
for log in bluecrab-app.log bluecrab-auth.log bluecrab-error.log; do
    if [ -f "$LOG_DIR/$log" ]; then
        size=$(du -h "$LOG_DIR/$log" | cut -f1)
        lines=$(wc -l < "$LOG_DIR/$log")
        modified=$(date -r "$LOG_DIR/$log" '+%Y-%m-%d %H:%M:%S')
        echo "  ✅ $log: ${size}, ${lines}줄, 수정: $modified"
    else
        echo "  ❌ $log: 파일이 존재하지 않음"
    fi
done

echo
echo "🔍 최근 에러 로그 (최근 10개):"
if [ -f "$LOG_DIR/bluecrab-error.log" ]; then
    tail -10 "$LOG_DIR/bluecrab-error.log"
else
    echo "에러 로그 파일이 없습니다."
fi

echo
echo "📊 최근 1시간 로그인 시도:"
if [ -f "$LOG_DIR/bluecrab-auth.log" ]; then
    grep "Login attempt" "$LOG_DIR/bluecrab-auth.log" | tail -20
else
    echo "인증 로그 파일이 없습니다."
fi
EOF

chmod +x /home/admin/bluecrab-monitor.sh
```

### 실행 방법
```bash
# 모니터링 스크립트 실행
/home/admin/bluecrab-monitor.sh

# 주기적 모니터링 (5분마다)
watch -n 300 /home/admin/bluecrab-monitor.sh
```

## 🔧 로그 설정 확인

### 현재 로그 경로 확인
```bash
# 톰캣 프로세스에서 로그 경로 확인
ps aux | grep tomcat
echo $CATALINA_BASE

# 실제 로그 파일 위치 확인
find / -name "bluecrab-*.log" 2>/dev/null
```

### 로그 레벨 동적 변경 (JMX 사용 시)
```bash
# Log4j2 JMX를 통한 로그 레벨 변경 가능
# (애플리케이션에서 JMX 활성화 필요)
```

## 📈 성능 모니터링

### 로그 기반 성능 분석
```bash
# 느린 요청 감지 (1초 이상)
grep "소요시간.*[0-9][0-9][0-9][0-9]ms" $CATALINA_BASE/logs/bluecrab-app.log

# 에러율 계산
total_requests=$(grep -c "요청 완료" $CATALINA_BASE/logs/bluecrab-app.log)
error_requests=$(grep -c "상태: [45][0-9][0-9]" $CATALINA_BASE/logs/bluecrab-app.log)
echo "총 요청: $total_requests, 에러: $error_requests"
```

## 🚨 알림 설정 (선택사항)

### 에러 로그 알림 스크립트
```bash
# /home/admin/error-alert.sh
#!/bin/bash
ERROR_COUNT=$(grep "ERROR" $CATALINA_BASE/logs/bluecrab-error.log | wc -l)
if [ $ERROR_COUNT -gt 10 ]; then
    echo "BlueCrab 애플리케이션에서 $ERROR_COUNT 개의 에러가 발생했습니다!" | mail -s "BlueCrab 에러 알림" admin@company.com
fi
```

### Crontab 등록
```bash
# 10분마다 에러 체크
*/10 * * * * /home/admin/error-alert.sh
```
