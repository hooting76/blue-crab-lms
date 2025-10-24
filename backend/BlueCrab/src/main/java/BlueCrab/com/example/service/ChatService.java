package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.Consultation.ChatMessageDto;

import java.util.List;

/**
 * 채팅 메시지 관리 Service 인터페이스
 * Redis를 사용하여 실시간 채팅 메시지를 저장하고 관리합니다.
 * 
 * Redis Key Pattern: chat:room:{requestIdx}
 * Data Structure: List (FIFO)
 * TTL: 36 hours (상담 시작 시점부터)
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public interface ChatService {

    /**
     * 채팅 메시지를 Redis에 저장
     * 
     * @param message 저장할 채팅 메시지
     */
    void saveMessage(ChatMessageDto message);

    /**
     * 특정 상담의 최근 N개 메시지 조회
     * 
     * @param requestIdx 상담 요청 ID
     * @param count 조회할 메시지 개수 (기본: 50)
     * @return 채팅 메시지 목록 (최신순)
     */
    List<ChatMessageDto> getMessages(Long requestIdx, int count);

    /**
     * 특정 상담의 전체 메시지 조회
     * 
     * @param requestIdx 상담 요청 ID
     * @return 전체 채팅 메시지 목록
     */
    List<ChatMessageDto> getAllMessages(Long requestIdx);

    /**
     * 특정 상담의 메시지 개수 조회
     * 
     * @param requestIdx 상담 요청 ID
     * @return 메시지 개수
     */
    long getMessageCount(Long requestIdx);

    /**
     * Redis 키 TTL 설정 (36시간)
     * 상담 시작 시 또는 메시지 전송 시 호출하여 TTL 연장
     * 
     * @param requestIdx 상담 요청 ID
     */
    void setTTL(Long requestIdx);

    /**
     * 특정 사용자가 상담 참여자인지 확인
     * 
     * @param requestIdx 상담 요청 ID
     * @param userCode 사용자 USER_CODE
     * @return 참여자 여부
     */
    boolean isParticipant(Long requestIdx, String userCode);

    /**
     * Redis에서 채팅 메시지 삭제
     * 상담 종료 시 호출
     * 
     * @param requestIdx 상담 요청 ID
     */
    void deleteMessages(Long requestIdx);

    /**
     * 채팅 로그를 텍스트 파일 형식으로 포맷팅
     * MinIO 저장 전 텍스트 파일 생성에 사용
     * 
     * @param requestIdx 상담 요청 ID
     * @return 포맷된 채팅 로그 텍스트
     */
    String formatChatLog(Long requestIdx);
}
