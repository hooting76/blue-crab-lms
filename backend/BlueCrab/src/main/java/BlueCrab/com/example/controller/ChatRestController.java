package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.Consultation.ChatMessageDto;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.ChatService;
import BlueCrab.com.example.service.MinIOService;
import BlueCrab.com.example.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 채팅 REST API Controller
 * 채팅 메시지 조회 및 히스토리 다운로드 기능 제공
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;
    private final MinIOService minIOService;
    private final JwtUtil jwtUtil;
    private final UserTblRepository userTblRepository;

    public ChatRestController(
        ChatService chatService,
        MinIOService minIOService,
        JwtUtil jwtUtil,
        UserTblRepository userTblRepository
    ) {
        this.chatService = chatService;
        this.minIOService = minIOService;
        this.jwtUtil = jwtUtil;
        this.userTblRepository = userTblRepository;
    }

    /**
     * 채팅 메시지 목록 조회
     * Redis에 저장된 실시간 채팅 메시지를 조회합니다.
     * 
     * @param requestIdx 상담 요청 ID
     * @param token JWT 인증 토큰
     * @return 채팅 메시지 목록
     */
    @GetMapping("/messages/{requestIdx}")
    public ResponseEntity<?> getChatMessages(
        @PathVariable Long requestIdx,
        @RequestHeader("Authorization") String token
    ) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String userIdentifier = jwtUtil.getUserCode(actualToken);
            String userCode = resolveUserCode(actualToken, userIdentifier);

            if (userCode == null) {
                log.warn("채팅 메시지 조회 실패 - 사용자 식별 불가: requestIdx={}, identifier={}",
                    requestIdx, userIdentifier);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "message", "사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요."
                    ));
            }

            log.info("채팅 메시지 조회: requestIdx={}, userCode={}", requestIdx, userCode);

            if (!chatService.isParticipant(requestIdx, userCode)) {
                log.warn("채팅 참여자가 아님: requestIdx={}, userCode={}", requestIdx, userCode);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "success", false,
                        "message", "채팅 참여자만 조회할 수 있습니다."
                    ));
            }

            List<ChatMessageDto> messages = chatService.getAllMessages(requestIdx);

            Map<String, Object> data = Map.of(
                "requestIdx", requestIdx,
                "messageCount", messages.size(),
                "messages", messages
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "채팅 메시지 조회 완료",
                "data", data
            ));

        } catch (Exception e) {
            log.error("채팅 메시지 조회 실패: requestIdx={}", requestIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "채팅 메시지 조회에 실패했습니다.",
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * 채팅 히스토리 텍스트 파일 다운로드
     * 현재 Redis에 있는 메시지를 텍스트 파일로 다운로드합니다.
     * 
     * @param requestIdx 상담 요청 ID
     * @param token JWT 인증 토큰
     * @return 채팅 로그 파일
     */
    @PostMapping("/history/download/{requestIdx}")
    public ResponseEntity<?> downloadChatHistory(
        @PathVariable Long requestIdx,
        @RequestHeader("Authorization") String token
    ) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String userIdentifier = jwtUtil.getUserCode(actualToken);
            String userCode = resolveUserCode(actualToken, userIdentifier);

            if (userCode == null) {
                log.warn("채팅 히스토리 다운로드 실패 - 사용자 식별 불가: requestIdx={}, identifier={}",
                    requestIdx, userIdentifier);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "message", "사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요."
                    ));
            }

            log.info("채팅 히스토리 다운로드: requestIdx={}, userCode={}", requestIdx, userCode);

            if (!chatService.isParticipant(requestIdx, userCode)) {
                log.warn("채팅 참여자가 아님: requestIdx={}, userCode={}", requestIdx, userCode);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "success", false,
                        "message", "채팅 참여자만 다운로드할 수 있습니다."
                    ));
            }

            String chatLog = chatService.formatChatLog(requestIdx);

            byte[] bytes = chatLog.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(bytes);

            String fileName = String.format("chat-log-%d.txt", requestIdx);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(bytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            log.error("채팅 히스토리 다운로드 실패: requestIdx={}", requestIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "채팅 히스토리 다운로드에 실패했습니다.",
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * MinIO에 저장된 채팅 로그 다운로드
     * 상담 종료 후 MinIO에 아카이빙된 채팅 로그를 다운로드합니다.
     * 
     * @param requestIdx 상담 요청 ID
     * @param token JWT 인증 토큰
     * @return MinIO 채팅 로그 파일
     */
    @GetMapping("/archive/download/{requestIdx}")
    public ResponseEntity<?> downloadArchivedChatLog(
        @PathVariable Long requestIdx,
        @RequestHeader("Authorization") String token
    ) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String userIdentifier = jwtUtil.getUserCode(actualToken);
            String userCode = resolveUserCode(actualToken, userIdentifier);

            if (userCode == null) {
                log.warn("아카이브 채팅 로그 다운로드 실패 - 사용자 식별 불가: requestIdx={}, identifier={}",
                    requestIdx, userIdentifier);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "message", "사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요."
                    ));
            }

            log.info("아카이빙된 채팅 로그 다운로드: requestIdx={}, userCode={}", requestIdx, userCode);

            if (!chatService.isParticipant(requestIdx, userCode)) {
                log.warn("채팅 참여자가 아님: requestIdx={}, userCode={}", requestIdx, userCode);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "success", false,
                        "message", "채팅 참여자만 다운로드할 수 있습니다."
                    ));
            }

            String objectName = String.format("chat-log-%d-", requestIdx);

            try {
                InputStream inputStream = minIOService.downloadChatLog("consultation-chats", objectName);

                String fileName = String.format("archived-chat-log-%d.txt", requestIdx);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.setContentDispositionFormData("attachment", fileName);

                return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));

            } catch (Exception e) {
                log.warn("MinIO에서 파일 찾을 수 없음: requestIdx={}", requestIdx);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", "아카이빙된 채팅 로그를 찾을 수 없습니다."
                    ));
            }

        } catch (Exception e) {
            log.error("아카이빙된 채팅 로그 다운로드 실패: requestIdx={}", requestIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "채팅 로그 다운로드에 실패했습니다.",
                    "error", e.getMessage()
                ));
        }
    }

    private String resolveUserCode(String token, String emailOrUserCode) {
        String fromIdentifier = convertToUserCode(emailOrUserCode);
        if (fromIdentifier != null) {
            return fromIdentifier;
        }

        Integer userId = jwtUtil.extractUserId(token);
        if (userId != null) {
            return userTblRepository.findById(userId)
                .map(user -> user.getUserCode())
                .orElse(null);
        }

        log.warn("JWT에서 사용자 코드를 추출하지 못했습니다: identifier={}", emailOrUserCode);
        return null;
    }

    private String convertToUserCode(String emailOrUserCode) {
        if (emailOrUserCode == null || emailOrUserCode.isBlank()) {
            return null;
        }

        if (!emailOrUserCode.contains("@")) {
            return emailOrUserCode;
        }

        return userTblRepository.findByUserEmail(emailOrUserCode)
            .map(user -> user.getUserCode())
            .orElseGet(() -> {
                log.warn("사용자 이메일로 userCode 찾을 수 없음: {}", emailOrUserCode);
                return null;
            });
    }
}
