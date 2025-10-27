# 실제 사용 예제 모음

## JavaScript/Fetch API 예제

### 1. 게시글 생성 예제

```javascript
/**
 * 게시글 생성 함수
 * @param {Object} boardData - 게시글 데이터
 * @param {string} jwtToken - JWT 액세스 토큰
 * @returns {Promise<Object>} 생성된 게시글 정보
 */
async function createBoard(boardData, jwtToken) {
  try {
    const response = await fetch('/api/boards/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${jwtToken}`
      },
      body: JSON.stringify(boardData)
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    
    if (result.success) {
      console.log('게시글 생성 성공:', result.boardIdx);
      return result;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('게시글 생성 오류:', error);
    throw error;
  }
}

// 사용 예시
const boardData = {
  boardTitle: "새로운 공지사항",
  boardContent: "중요한 공지 내용입니다.",
  boardCode: 0
};

createBoard(boardData, localStorage.getItem('jwtToken'))
  .then(result => {
    alert('게시글이 성공적으로 작성되었습니다.');
    console.log('게시글 ID:', result.boardIdx);
  })
  .catch(error => {
    alert('게시글 작성에 실패했습니다: ' + error.message);
  });
```

### 2. 파일 업로드 + 게시글 생성 예제

```javascript
/**
 * 파일 업로드 + 게시글 생성 통합 함수
 * @param {Object} boardData - 게시글 데이터
 * @param {FileList} files - 업로드할 파일들
 * @param {string} jwtToken - JWT 토큰
 */
async function createBoardWithFiles(boardData, files, jwtToken) {
  try {
    let attachmentIdx = [];

    // 1. 파일이 있으면 먼저 업로드
    if (files && files.length > 0) {
      console.log('파일 업로드 시작...');
      attachmentIdx = await uploadFiles(files, jwtToken);
      console.log('파일 업로드 완료, IDX:', attachmentIdx);
    }

    // 2. 게시글 생성
    console.log('게시글 생성 시작...');
    const boardResult = await createBoard(boardData, jwtToken);
    const boardIdx = boardResult.boardIdx;

    // 3. 첨부파일이 있으면 연결
    if (attachmentIdx.length > 0) {
      console.log('첨부파일 연결 시작...');
      await linkAttachments(boardIdx, attachmentIdx, jwtToken);
      console.log('첨부파일 연결 완료');
    }

    return {
      boardIdx: boardIdx,
      attachmentCount: attachmentIdx.length,
      message: '게시글과 첨부파일이 성공적으로 업로드되었습니다.'
    };

  } catch (error) {
    console.error('전체 프로세스 오류:', error);
    throw error;
  }
}

/**
 * 파일 업로드 함수
 */
async function uploadFiles(files, jwtToken) {
  const formData = new FormData();
  
  Array.from(files).forEach(file => {
    formData.append('files', file);
  });

  const response = await fetch('/api/board-attachments/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${jwtToken}`
    },
    body: formData
  });

  const result = await response.json();
  
  if (result.success) {
    return result.attachmentIdx;
  } else {
    throw new Error(result.message);
  }
}

/**
 * 첨부파일 연결 함수
 */
async function linkAttachments(boardIdx, attachmentIdx, jwtToken) {
  const response = await fetch(`/api/boards/link-attachments/${boardIdx}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${jwtToken}`
    },
    body: JSON.stringify({ attachmentIdx })
  });

  const result = await response.json();
  
  if (!result.success) {
    throw new Error(result.message);
  }
  
  return result;
}

// HTML에서 사용 예시
document.getElementById('boardForm').addEventListener('submit', async function(e) {
  e.preventDefault();
  
  const formData = new FormData(this);
  const files = document.getElementById('fileInput').files;
  
  const boardData = {
    boardTitle: formData.get('title'),
    boardContent: formData.get('content'),
    boardCode: parseInt(formData.get('boardCode')),
    lecSerial: formData.get('lecSerial') || null
  };

  try {
    const result = await createBoardWithFiles(
      boardData, 
      files, 
      localStorage.getItem('jwtToken')
    );
    
    alert(result.message);
    location.reload(); // 페이지 새로고침
  } catch (error) {
    alert('오류: ' + error.message);
  }
});
```

### 3. 게시글 목록 조회 예제

```javascript
/**
 * 게시글 목록 조회 함수
 * @param {Object} filters - 필터 조건
 * @returns {Promise<Object>} 게시글 목록과 페이징 정보
 */
async function fetchBoardList(filters = {}) {
  try {
    const response = await fetch('/api/boards/list', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(filters)
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('목록 조회 오류:', error);
    throw error;
  }
}

/**
 * 게시글 목록 렌더링 함수
 */
function renderBoardList(boardList, containerId) {
  const container = document.getElementById(containerId);
  
  if (!boardList.content || boardList.content.length === 0) {
    container.innerHTML = '<p>게시글이 없습니다.</p>';
    return;
  }

  const html = boardList.content.map(board => `
    <div class="board-item" data-board-idx="${board.boardIdx}">
      <h3 class="board-title">${board.boardTitle}</h3>
      <div class="board-meta">
        <span class="author">작성자: ${board.boardWriter}</span>
        <span class="date">작성일: ${board.boardReg}</span>
        <span class="views">조회수: ${board.boardView}</span>
        ${board.boardFile ? '<span class="attachment">📎</span>' : ''}
      </div>
    </div>
  `).join('');

  container.innerHTML = html;

  // 클릭 이벤트 추가
  container.querySelectorAll('.board-item').forEach(item => {
    item.addEventListener('click', function() {
      const boardIdx = this.dataset.boardIdx;
      showBoardDetail(boardIdx);
    });
  });
}

/**
 * 페이징 렌더링 함수
 */
function renderPagination(pageInfo, containerId, onPageChange) {
  const container = document.getElementById(containerId);
  const currentPage = pageInfo.number;
  const totalPages = pageInfo.totalPages;

  let html = '<div class="pagination">';
  
  // 이전 페이지
  if (currentPage > 0) {
    html += `<button onclick="${onPageChange}(${currentPage - 1})">이전</button>`;
  }
  
  // 페이지 번호들
  for (let i = Math.max(0, currentPage - 2); 
       i <= Math.min(totalPages - 1, currentPage + 2); 
       i++) {
    const active = i === currentPage ? 'active' : '';
    html += `<button class="${active}" onclick="${onPageChange}(${i})">${i + 1}</button>`;
  }
  
  // 다음 페이지
  if (currentPage < totalPages - 1) {
    html += `<button onclick="${onPageChange}(${currentPage + 1})">다음</button>`;
  }
  
  html += '</div>';
  container.innerHTML = html;
}

// 사용 예시
let currentFilters = { boardCode: 0, page: 0, size: 10 };

function loadBoardList(page = 0) {
  currentFilters.page = page;
  
  fetchBoardList(currentFilters)
    .then(result => {
      renderBoardList(result, 'boardListContainer');
      renderPagination(result, 'paginationContainer', 'loadBoardList');
    })
    .catch(error => {
      console.error('목록 로딩 실패:', error);
      alert('게시글 목록을 불러오는데 실패했습니다.');
    });
}

// 페이지 로딩 시 초기 목록 로드
document.addEventListener('DOMContentLoaded', function() {
  loadBoardList();
});
```

### 4. 게시글 상세 조회 예제

```javascript
/**
 * 게시글 상세 조회 함수
 * @param {number} boardIdx - 게시글 ID
 * @returns {Promise<Object>} 게시글 상세 정보
 */
async function fetchBoardDetail(boardIdx) {
  try {
    const response = await fetch('/api/boards/detail', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ boardIdx })
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('게시글을 찾을 수 없습니다.');
      }
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('상세 조회 오류:', error);
    throw error;
  }
}

/**
 * 게시글 상세 정보 렌더링 함수
 */
function renderBoardDetail(board, containerId) {
  const container = document.getElementById(containerId);
  
  const attachmentsHtml = board.attachmentDetails && board.attachmentDetails.length > 0
    ? `
      <div class="attachments">
        <h4>첨부파일</h4>
        <ul>
          ${board.attachmentDetails.map(file => `
            <li>
              <a href="/api/board-attachments/download/${file.attachmentIdx}" 
                 download="${file.originalName}">
                📎 ${file.originalName} (${formatFileSize(file.fileSize)})
              </a>
            </li>
          `).join('')}
        </ul>
      </div>
    `
    : '';

  const html = `
    <div class="board-detail">
      <header class="board-header">
        <h1 class="board-title">${board.boardTitle}</h1>
        <div class="board-meta">
          <span class="author">작성자: ${board.boardWriter}</span>
          <span class="date">작성일: ${board.boardReg}</span>
          <span class="views">조회수: ${board.boardView}</span>
          ${board.lecSerial ? `<span class="lecture">강의: ${board.lecSerial}</span>` : ''}
        </div>
      </header>
      
      <div class="board-content">
        ${board.boardContent.replace(/\n/g, '<br>')}
      </div>
      
      ${attachmentsHtml}
      
      <div class="board-actions">
        <button onclick="goToList()">목록으로</button>
        ${canEdit(board) ? `<button onclick="editBoard(${board.boardIdx})">수정</button>` : ''}
        ${canDelete(board) ? `<button onclick="deleteBoard(${board.boardIdx})">삭제</button>` : ''}
      </div>
    </div>
  `;

  container.innerHTML = html;
}

/**
 * 파일 크기 포맷팅 함수
 */
function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * 편집 권한 확인 함수
 */
function canEdit(board) {
  // 실제 구현에서는 현재 사용자 정보와 비교
  const currentUser = getCurrentUser();
  return currentUser && (
    currentUser.userType === 1 || // 관리자
    currentUser.userIdx === board.boardWriterIdx // 작성자
  );
}

/**
 * 삭제 권한 확인 함수
 */
function canDelete(board) {
  return canEdit(board); // 편집 권한과 동일
}

// 사용 예시
function showBoardDetail(boardIdx) {
  fetchBoardDetail(boardIdx)
    .then(board => {
      renderBoardDetail(board, 'boardDetailContainer');
      
      // 브라우저 히스토리 추가
      history.pushState({boardIdx}, '', `/board/${boardIdx}`);
    })
    .catch(error => {
      alert('게시글을 불러오는데 실패했습니다: ' + error.message);
    });
}

// 뒤로가기 지원
window.addEventListener('popstate', function(event) {
  if (event.state && event.state.boardIdx) {
    showBoardDetail(event.state.boardIdx);
  } else {
    goToList();
  }
});
```

## PowerShell 스크립트 예제

### 1. 배치 테스트 스크립트

```powershell
# board-test.ps1
# 게시판 API 테스트 스크립트

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$JwtToken = ""
)

# JWT 토큰 확인
if (-not $JwtToken) {
    Write-Host "JWT 토큰을 입력하세요:" -ForegroundColor Yellow
    $JwtToken = Read-Host
}

# 테스트 데이터
$TestBoards = @(
    @{
        boardTitle = "테스트 공지사항 1"
        boardContent = "첫 번째 테스트 공지사항입니다."
        boardCode = 0
    },
    @{
        boardTitle = "ETH201 과제 안내"
        boardContent = "이더리움 과제를 다음 주까지 제출하세요."
        boardCode = 3
        lecSerial = "ETH201"
    },
    @{
        boardTitle = "시설 이용 안내"
        boardContent = "도서관 이용 시간이 변경되었습니다."
        boardCode = 2
    }
)

# 게시글 생성 테스트
function Test-CreateBoard {
    param($BoardData)
    
    $Json = $BoardData | ConvertTo-Json -Depth 3
    
    try {
        $Response = Invoke-RestMethod -Uri "$BaseUrl/api/boards/create" `
            -Method POST `
            -Headers @{
                "Content-Type" = "application/json"
                "Authorization" = "Bearer $JwtToken"
            } `
            -Body $Json
        
        if ($Response.success) {
            Write-Host "✅ 게시글 생성 성공: ID $($Response.boardIdx)" -ForegroundColor Green
            return $Response.boardIdx
        } else {
            Write-Host "❌ 게시글 생성 실패: $($Response.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ API 호출 오류: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    return $null
}

# 게시글 목록 조회 테스트
function Test-GetBoardList {
    param($BoardCode = $null)
    
    $Body = @{}
    if ($BoardCode -ne $null) {
        $Body.boardCode = $BoardCode
    }
    
    $Json = $Body | ConvertTo-Json -Depth 2
    
    try {
        $Response = Invoke-RestMethod -Uri "$BaseUrl/api/boards/list" `
            -Method POST `
            -Headers @{"Content-Type" = "application/json"} `
            -Body $Json
        
        Write-Host "✅ 목록 조회 성공: 총 $($Response.totalElements)개 게시글" -ForegroundColor Green
        return $Response
    } catch {
        Write-Host "❌ 목록 조회 실패: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    return $null
}

# 테스트 실행
Write-Host "🚀 게시판 API 테스트 시작" -ForegroundColor Cyan

# 1. 게시글 생성 테스트
Write-Host "`n📝 게시글 생성 테스트" -ForegroundColor Yellow
$CreatedIds = @()
foreach ($Board in $TestBoards) {
    $BoardId = Test-CreateBoard -BoardData $Board
    if ($BoardId) {
        $CreatedIds += $BoardId
    }
    Start-Sleep -Seconds 1
}

# 2. 목록 조회 테스트
Write-Host "`n📋 목록 조회 테스트" -ForegroundColor Yellow
Test-GetBoardList
Test-GetBoardList -BoardCode 0
Test-GetBoardList -BoardCode 3

# 3. 상세 조회 테스트
Write-Host "`n🔍 상세 조회 테스트" -ForegroundColor Yellow
foreach ($BoardId in $CreatedIds) {
    try {
        $DetailBody = @{ boardIdx = $BoardId } | ConvertTo-Json
        $Response = Invoke-RestMethod -Uri "$BaseUrl/api/boards/detail" `
            -Method POST `
            -Headers @{"Content-Type" = "application/json"} `
            -Body $DetailBody
        
        Write-Host "✅ 상세 조회 성공: $($Response.boardTitle)" -ForegroundColor Green
    } catch {
        Write-Host "❌ 상세 조회 실패: $($_.Exception.Message)" -ForegroundColor Red
    }
    Start-Sleep -Seconds 1
}

Write-Host "`n🎉 테스트 완료" -ForegroundColor Cyan
```

### 2. 파일 업로드 테스트 스크립트

```powershell
# file-upload-test.ps1

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$JwtToken = "",
    [string]$FilePath = ""
)

if (-not $JwtToken) {
    $JwtToken = Read-Host "JWT 토큰을 입력하세요"
}

if (-not $FilePath -or -not (Test-Path $FilePath)) {
    Write-Host "업로드할 파일 경로를 입력하세요:" -ForegroundColor Yellow
    $FilePath = Read-Host
}

# 파일 업로드
try {
    $FileItem = Get-Item $FilePath
    
    # multipart/form-data 요청 생성
    $boundary = [System.Guid]::NewGuid().ToString()
    $bodyLines = @(
        "--$boundary",
        'Content-Disposition: form-data; name="files"; filename="' + $FileItem.Name + '"',
        'Content-Type: application/octet-stream',
        '',
        [System.IO.File]::ReadAllText($FilePath),
        "--$boundary--"
    )
    
    $body = $bodyLines -join "`r`n"
    
    $Response = Invoke-RestMethod -Uri "$BaseUrl/api/board-attachments/upload" `
        -Method POST `
        -Headers @{
            "Authorization" = "Bearer $JwtToken"
            "Content-Type" = "multipart/form-data; boundary=$boundary"
        } `
        -Body $body
    
    if ($Response.success) {
        Write-Host "✅ 파일 업로드 성공" -ForegroundColor Green
        Write-Host "업로드된 파일 IDX: $($Response.attachmentIdx -join ', ')" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ 파일 업로드 실패: $($_.Exception.Message)" -ForegroundColor Red
}
```

## Python 예제

### requests 라이브러리 사용 예제

```python
# board_api_client.py
import requests
import json
from typing import Optional, List, Dict, Any

class BoardAPIClient:
    def __init__(self, base_url: str = "http://localhost:8080", jwt_token: str = ""):
        self.base_url = base_url
        self.jwt_token = jwt_token
        self.session = requests.Session()
        
        if jwt_token:
            self.session.headers.update({
                'Authorization': f'Bearer {jwt_token}'
            })
    
    def create_board(self, title: str, content: str, board_code: int, 
                    lec_serial: Optional[str] = None) -> Dict[str, Any]:
        """게시글 생성"""
        data = {
            'boardTitle': title,
            'boardContent': content,
            'boardCode': board_code
        }
        
        if lec_serial:
            data['lecSerial'] = lec_serial
        
        response = self.session.post(
            f'{self.base_url}/api/boards/create',
            headers={'Content-Type': 'application/json'},
            json=data
        )
        
        response.raise_for_status()
        return response.json()
    
    def get_board_list(self, board_code: Optional[int] = None, 
                      lec_serial: Optional[str] = None,
                      page: int = 0, size: int = 10) -> Dict[str, Any]:
        """게시글 목록 조회"""
        data = {
            'page': page,
            'size': size
        }
        
        if board_code is not None:
            data['boardCode'] = board_code
        
        if lec_serial:
            data['lecSerial'] = lec_serial
        
        response = requests.post(
            f'{self.base_url}/api/boards/list',
            headers={'Content-Type': 'application/json'},
            json=data
        )
        
        response.raise_for_status()
        return response.json()
    
    def get_board_detail(self, board_idx: int) -> Dict[str, Any]:
        """게시글 상세 조회"""
        response = requests.post(
            f'{self.base_url}/api/boards/detail',
            headers={'Content-Type': 'application/json'},
            json={'boardIdx': board_idx}
        )
        
        response.raise_for_status()
        return response.json()
    
    def upload_files(self, file_paths: List[str]) -> List[int]:
        """파일 업로드"""
        files = []
        
        try:
            for file_path in file_paths:
                files.append(('files', open(file_path, 'rb')))
            
            response = self.session.post(
                f'{self.base_url}/api/board-attachments/upload',
                files=files
            )
            
            response.raise_for_status()
            result = response.json()
            
            if result['success']:
                return result['attachmentIdx']
            else:
                raise Exception(result['message'])
                
        finally:
            # 파일 핸들 정리
            for _, file_handle in files:
                file_handle.close()

# 사용 예시
if __name__ == "__main__":
    client = BoardAPIClient(jwt_token="your-jwt-token-here")
    
    try:
        # 게시글 생성
        result = client.create_board(
            title="Python에서 작성한 공지사항",
            content="Python API 클라이언트 테스트입니다.",
            board_code=0
        )
        
        print(f"게시글 생성 성공: ID {result['boardIdx']}")
        
        # 목록 조회
        board_list = client.get_board_list(board_code=0)
        print(f"총 {board_list['totalElements']}개의 공지사항이 있습니다.")
        
        # 상세 조회
        detail = client.get_board_detail(result['boardIdx'])
        print(f"제목: {detail['boardTitle']}")
        print(f"조회수: {detail['boardView']}")
        
    except requests.exceptions.RequestException as e:
        print(f"API 호출 오류: {e}")
    except Exception as e:
        print(f"처리 오류: {e}")
```

이러한 예제들을 통해 다양한 환경에서 게시판 API를 활용할 수 있습니다.