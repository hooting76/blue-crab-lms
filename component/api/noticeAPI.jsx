const BASE_URL = 'https://bluecrab.chickenkiller.com/Bluecrab-1.0.0/api/boards';

// API 요청에 사용할 공통 헤더
const getHeaders = (accessToken) => ({
  'Authorization': `Bearer ${accessToken}`,
  'Content-Type': 'application/json'
});

// 게시글 목록 조회 (페이징)
const getNotices = async (accessToken, page = 0, size = 10) => {
  try {
    const response = await fetch(`${BASE_URL}/list?page=${page}&size=${size}`, {
      headers: getHeaders(accessToken)
    });
    if (!response.ok) throw new Error('게시글 목록을 불러오는데 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('게시글 목록 조회 에러:', error);
    throw error;
  }
};

// 특정 게시글 상세 조회
export const getNoticeDetail = async (accessToken, boardIdx) => {
  try {
    const response = await fetch(`${BASE_URL}/${boardIdx}`, {
      headers: getHeaders(accessToken)
    });
    if (!response.ok) throw new Error('게시글을 불러오는데 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('게시글 상세 조회 에러:', error);
    throw error;
  }
};

// 게시글 작성
export const createNotice = async (accessToken, noticeData) => {
  try {
    const response = await fetch(`${BASE_URL}/create`, {
      method: 'POST',
      headers: getHeaders(accessToken),
      body: JSON.stringify(noticeData)
    });
    if (!response.ok) throw new Error('게시글 작성에 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('게시글 작성 에러:', error);
    throw error;
  }
};

// 게시글 수정
export const updateNotice = async (accessToken, boardIdx, updateData) => {
  try {
    const response = await fetch(`${BASE_URL}/update/${boardIdx}`, {
      method: 'PUT',
      headers: getHeaders(accessToken),
      body: JSON.stringify(updateData)
    });
    if (!response.ok) throw new Error('게시글 수정에 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('게시글 수정 에러:', error);
    throw error;
  }
};

// 게시글 삭제
export const deleteNotice = async (accessToken, boardIdx) => {
  try {
    const response = await fetch(`${BASE_URL}/delete/${boardIdx}`, {
      method: 'DELETE',
      headers: getHeaders(accessToken)
    });
    if (!response.ok) throw new Error('게시글 삭제에 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('게시글 삭제 에러:', error);
    throw error;
  }
};

// 특정 코드의 게시글 목록 조회
export const getNoticesByCode = async (accessToken, boardCode, page = 0, size = 10) => {
  try {
    const response = await fetch(`${BASE_URL}/bycode/${boardCode}?page=${page}&size=${size}`, {
      headers: getHeaders(accessToken)
    });
    if (!response.ok) throw new Error('게시글 목록을 불러오는데 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('코드별 게시글 목록 조회 에러:', error);
    throw error;
  }
};

export default getNotices;