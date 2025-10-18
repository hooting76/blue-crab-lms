# 📦 참고 데이터

테스트에 필요한 참고 데이터 파일들입니다.

## 📁 파일 목록

### `professor_accounts.csv`
**교수 계정 정보**

Blue Crab LMS에 등록된 교수 계정의 정보를 포함합니다.

#### 데이터 구조
```csv
교수명, 이메일, 소속, 담당 과목
홍길동, hong@example.com, 컴퓨터공학과, 데이터베이스
김철수, kim@example.com, 전자공학과, 회로이론
...
```

#### 사용 목적
- 테스트용 교수 계정 확인
- 강의 생성 시 담당 교수 지정
- 권한 테스트 시 교수 계정 선택

## 🚀 사용 방법

### 1. 파일 확인
```bash
# CSV 파일 열기
notepad professor_accounts.csv
```

### 2. 테스트 계정 선택
```javascript
// 교수 계정으로 로그인
const professorEmail = 'hong@example.com'
const password = 'testpass123'

await login()
// 이메일 입력: hong@example.com
// 비밀번호 입력: testpass123
```

### 3. 강의 생성 시 사용
```javascript
const lectureData = {
    lecTitle: "데이터베이스 설계",
    professorIdx: 22,  // CSV에서 확인한 교수 IDX
    lecSerial: "CS301",
    ...
}
```

## ⚠️ 주의사항

- **실제 계정**: CSV의 계정은 실제 DB에 등록된 계정입니다
- **비밀번호**: 보안상 비밀번호는 별도 관리 (CSV에 미포함)
- **권한**: 교수 계정만 강의 생성/과제 채점 가능
- **데이터 갱신**: 새 교수 추가 시 CSV 업데이트 필요

## 🔗 관련 문서

- [상위 README](../README.md)
- [교수 테스트](../03-professor/)
- [관리자 테스트](../01-admin/)
