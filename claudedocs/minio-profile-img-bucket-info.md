# MinIO 프로필 이미지 버킷 정보

조회 일시: 2025-10-21

## 서버 환경

### MinIO 서버 정보
- **설치 위치**: `/home/project01/minio` (바이너리 파일)
- **버전**: RELEASE.2025-09-07T16-13-09Z (go1.24.6 linux/amd64)
- **데이터 디렉토리**: `/home/project01/minio-data`
- **실행 방식**: `nohup ~/minio server --console-address :9001 ~/minio-data &`

### API 엔드포인트
- **API**:
  - http://192.168.0.3:9000
  - http://172.17.0.1:9000
  - http://127.0.0.1:9000

- **WebUI (Console)**:
  - http://192.168.0.3:9001
  - http://172.17.0.1:9001
  - http://127.0.0.1:9001

### 인증 정보
- **기본 자격증명**: `minioadmin:minioadmin` (변경 권장)
- **환경변수**: `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`로 변경 가능

## 버킷 구조

### 전체 버킷 목록
```
/home/project01/minio-data/
├── .minio.sys/          # MinIO 시스템 디렉토리
├── board-attached/      # 게시판 첨부파일 버킷
├── profile-img/         # 프로필 이미지 버킷 ⭐
└── test/                # 테스트 버킷
```

## profile-img 버킷 상세 정보

### 기본 정보
- **버킷 이름**: `profile-img`
- **물리적 경로**: `/home/project01/minio-data/profile-img/`
- **생성일**: 2025-09-24 (최종 메타데이터 수정일)
- **총 용량**: 96KB
- **저장된 파일 수**: 1개

### 스토리지 형식
- **스토리지 형식**: `xl-single` (Erasure Coding 단일 드라이브 모드)
- **버전**: xl v3
- **분산 알고리즘**: SIPMOD+PARITY
- **포맷 ID**: `e37d3d90-129b-492c-afb2-004e8fd2bc72`

### 저장된 파일 예시

#### 파일 구조
MinIO는 erasure coding을 사용하여 객체를 저장합니다:
```
/home/project01/minio-data/profile-img/
└── 202500101000_20250925173216.jpg/    # 객체 디렉토리
    └── xl.meta                          # 메타데이터 + 실제 데이터 (85KB)
```

#### 샘플 파일
- **객체명**: `202500101000_20250925173216.jpg`
- **업로드 일시**: 2025-09-25 05:17 (UTC+9 기준 17:32)
- **파일 크기**: 85KB
- **저장 형식**: xl.meta 파일에 메타데이터와 실제 이미지 데이터가 함께 저장됨

### 파일명 규칙 분석
현재 저장된 파일명: `202500101000_20250925173216.jpg`
- **추정 패턴**: `{학번}_{업로드타임스탬프}.jpg`
  - `202500101000`: 학번으로 추정
  - `20250925173216`: YYYYMMDDHHMMSS 형식의 타임스탬프
  - `.jpg`: 파일 확장자

## 버킷 메타데이터

### 시스템 메타데이터 위치
```
/home/project01/minio-data/.minio.sys/buckets/profile-img/
├── .metadata.bin/           # 버킷 메타데이터
├── .usage-cache.bin/        # 사용량 캐시
└── .usage-cache.bin.bkp/    # 사용량 캐시 백업
```

## 버킷 정책 및 설정

### 접근 제어
- 현재 조회 시점에는 MinIO 서버가 실행 중이 아님 (TERMINATED 상태)
- 버킷 정책은 MinIO 실행 후 WebUI 또는 mc 클라이언트로 확인 필요

### 권장 설정 사항

#### 1. 보안 강화
```bash
# 환경변수로 관리자 계정 변경
export MINIO_ROOT_USER=your-admin-username
export MINIO_ROOT_PASSWORD=your-secure-password

# MinIO 재시작
nohup ~/minio server --console-address :9001 ~/minio-data &
```

#### 2. 버킷 정책 예시 (프로필 이미지용)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::profile-img/*"]
    }
  ]
}
```
- 읽기 전용 공개 접근 (프로필 이미지 조회용)
- 업로드/삭제는 인증된 사용자만 가능

#### 3. 라이프사이클 정책 권장
- 오래된 프로필 이미지 자동 정리
- 썸네일 자동 생성 및 최적화
- 백업 정책 수립

## API 사용 예시

### Java Spring Boot에서 MinIO 사용
```java
// MinIO 클라이언트 설정
MinioClient minioClient = MinioClient.builder()
    .endpoint("http://192.168.0.3:9000")
    .credentials("minioadmin", "minioadmin")
    .build();

// 프로필 이미지 업로드
String objectName = studentId + "_" + timestamp + ".jpg";
minioClient.putObject(
    PutObjectArgs.builder()
        .bucket("profile-img")
        .object(objectName)
        .stream(inputStream, size, -1)
        .contentType("image/jpeg")
        .build()
);

// 프로필 이미지 URL 생성
String url = minioClient.getPresignedObjectUrl(
    GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket("profile-img")
        .object(objectName)
        .expiry(7, TimeUnit.DAYS)
        .build()
);
```

## 참고 사항

1. **현재 상태**: MinIO 서버가 중지된 상태 (마지막 종료: SIGTERM 수신)
2. **재시작 필요**: 프로필 이미지 업로드/조회 기능 사용 시 MinIO 서버 재시작 필요
3. **모니터링**: WebUI (http://192.168.0.3:9001)에서 버킷 상태 및 사용량 모니터링 가능
4. **백업**: 물리적 경로(`/home/project01/minio-data/profile-img/`)를 정기적으로 백업 권장

## 다음 단계

- [ ] MinIO 관리자 계정 변경 (보안 강화)
- [ ] 버킷 정책 설정 (읽기 전용 공개 접근)
- [ ] 파일 크기 제한 설정 (예: 최대 5MB)
- [ ] 이미지 최적화 파이프라인 구축
- [ ] 정기 백업 스크립트 작성
- [ ] 모니터링 및 알림 설정
