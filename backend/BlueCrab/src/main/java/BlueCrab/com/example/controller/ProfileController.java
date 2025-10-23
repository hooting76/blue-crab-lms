package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.AddressUpdateRequest;
import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.ImageRequest;
import BlueCrab.com.example.entity.ProfileView;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.service.ProfileService;
import BlueCrab.com.example.service.MinIOService;
import BlueCrab.com.example.service.ImageCacheService;
import BlueCrab.com.example.service.UserTblService;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.InputStream;
import java.util.Map;

/**
 * 사용자 프로필 관리를 위한 REST API 컨트롤러
 * JWT 토큰 기반 인증을 통해 본인의 프로필 정보만 조회 가능
 *
 * 주요 기능:
 * - 내 프로필 정보 조회 (/api/profile/me)
 * - 프로필 완성도 체크 (/api/profile/me/completeness)
 * - 프로필 이미지 파일 조회 (POST /api/profile/me/image/file)
 *
 * 보안 사항:
 * - JWT 토큰을 통한 인증 필수
 * - 본인의 프로필 정보만 접근 가능
 * - Authorization 헤더에서 Bearer 토큰 추출
 *
 * 응답 형식:
 * 모든 응답은 ApiResponse<T> 형식으로 통일:
 * {
 *   "success": true/false,
 *   "message": "한국어 메시지",
 *   "data": 실제_데이터,
 *   "timestamp": "2024-XX-XX..."
 * }
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private ProfileService profileService;

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private ImageCacheService imageCacheService;

    @Autowired
    private UserTblService userTblService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.domain}")
    private String appDomain;

    /**
     * 내 프로필 정보 조회
     * JWT 토큰에서 사용자 정보를 추출하여 해당 사용자의 프로필 정보를 조회
     *
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @return 사용자의 종합 프로필 정보
     *
     * 요청 예시:
     * POST /api/profile/me
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "프로필 정보를 성공적으로 조회했습니다.",
     *   "data": {
     *     "userEmail": "test@bluecrab.com",
     *     "userName": "테스트 사용자",
     *     "userPhone": "01087654321",
     *     "userType": 1,
     *     "majorCode": "202500101000",
     *     "zipCode": "12345",
     *     "mainAddress": "서울특별시 강남구",
     *     "detailAddress": "테헤란로 123",
     *     "profileImageKey": "profile_123.jpg",
     *     "birthDate": "19950315",
     *     "academicStatus": "재학",
     *     "admissionRoute": "정시",
     *     "majorFacultyCode": "F001",
     *     "majorDeptCode": "D001",
     *     "minorFacultyCode": null,
     *     "minorDeptCode": null
     *   },
     *   "timestamp": "2024-08-27T10:30:00Z"
     * }
     */
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(HttpServletRequest request) {
        try {
            // JWT 토큰에서 사용자 이메일 추출
            String userEmail = extractUserEmailFromToken(request);

            logger.info("프로필 조회 요청 - 사용자: {}", userEmail);

            // 프로필 정보 조회
            ProfileView profile = profileService.getMyProfile(userEmail);

            // 프로필 이미지 URL 생성하여 응답 데이터에 추가
            Map<String, Object> profileData = createProfileResponseWithImage(profile);

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "프로필 정보를 성공적으로 조회했습니다.", profileData);

            logger.info("프로필 조회 성공 - 사용자: {}, 이름: {}", userEmail, profile.getUserName());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("프로필 조회 실패: {}", e.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            logger.error("프로필 조회 중 시스템 오류 발생: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = ApiResponse.failure("프로필 정보 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 프로필 완성도 체크
     * 사용자의 프로필 정보가 얼마나 완성되었는지 확인
     *
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @return 프로필 완성도 정보
     *
     * 요청 예시:
     * POST /api/profile/me/completeness
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "프로필 완성도를 확인했습니다.",
     *   "data": {
     *     "complete": false,
     *     "message": "다음 정보가 누락되었습니다: 전화번호",
     *     "completionRate": 80
     *   },
     *   "timestamp": "2024-08-27T10:30:00Z"
     * }
     */
    @PostMapping("/me/completeness")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkProfileCompleteness(HttpServletRequest request) {
        try {
            // JWT 토큰에서 사용자 이메일 추출
            String userEmail = extractUserEmailFromToken(request);

            logger.debug("프로필 완성도 체크 요청 - 사용자: {}", userEmail);

            // 프로필 정보 조회
            ProfileView profile = profileService.getMyProfile(userEmail);

            // 완성도 체크
            ProfileService.ProfileCompleteness completeness = profileService.checkProfileCompleteness(profile);

            Map<String, Object> data = Map.of(
                "complete", completeness.isComplete(),
                "message", completeness.getMessage(),
                "userType", profile.getUserTypeText(),
                "hasAddress", profile.getFullAddress() != null && !profile.getFullAddress().trim().isEmpty(),
                "hasMajorInfo", profile.hasMajorInfo(),
                "hasMinorInfo", profile.hasMinorInfo(),
                "hasProfileImage", profile.getProfileImageKey() != null && !profile.getProfileImageKey().trim().isEmpty()
            );

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "프로필 완성도를 확인했습니다.", data);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("프로필 완성도 체크 실패: {}", e.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            logger.error("프로필 완성도 체크 중 시스템 오류 발생: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = ApiResponse.failure("프로필 완성도 확인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 주소 정보 업데이트
     * JWT 인증된 사용자의 주소 정보를 업데이트
     *
     * @param addressRequest 주소 업데이트 요청 정보
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @return 업데이트된 주소 정보
     *
     * 요청 예시:
     * POST /api/profile/address/update
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * Content-Type: application/json
     * {
     *   "postalCode": "05852",
     *   "roadAddress": "서울 송파구 위례광장로 120",
     *   "detailAddress": "장지동, 위례중앙푸르지오1단지"
     * }
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "주소가 성공적으로 업데이트되었습니다.",
     *   "data": {
     *     "zipCode": "05852",
     *     "mainAddress": "서울 송파구 위례광장로 120",
     *     "detailAddress": "장지동, 위례중앙푸르지오1단지"
     *   },
     *   "timestamp": "2025-10-23T15:30:00Z"
     * }
     */
    @PostMapping("/address/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAddress(
            @Valid @RequestBody AddressUpdateRequest addressRequest,
            HttpServletRequest request) {

        try {
            // JWT 토큰에서 사용자 이메일 추출
            String userEmail = extractUserEmailFromToken(request);

            logger.info("주소 업데이트 요청 - 사용자: {}, 우편번호: {}",
                       userEmail, addressRequest.getPostalCode());

            // 서비스 호출하여 주소 업데이트
            userTblService.updateUserAddress(
                userEmail,
                addressRequest.getPostalCode(),
                addressRequest.getRoadAddress(),
                addressRequest.getDetailAddress()
            );

            // 업데이트된 주소 정보 응답
            Map<String, Object> responseData = Map.of(
                "zipCode", addressRequest.getPostalCode(),
                "mainAddress", addressRequest.getRoadAddress(),
                "detailAddress", addressRequest.getDetailAddress() != null
                    ? addressRequest.getDetailAddress() : ""
            );

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "주소가 성공적으로 업데이트되었습니다.", responseData);

            logger.info("주소 업데이트 성공 - 사용자: {}", userEmail);

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException notFound) {
            logger.warn("주소 업데이트 실패(사용자 없음) - 오류: {}", notFound.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(notFound.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalArgumentException invalidRequest) {
            logger.warn("주소 업데이트 실패(잘못된 요청) - 오류: {}", invalidRequest.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(invalidRequest.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            logger.error("주소 업데이트 중 시스템 오류 발생: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(
                "주소 업데이트 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 프로필 이미지 업로드
     * JWT 인증된 사용자의 프로필 이미지를 업로드하고 기존 이미지를 교체
     *
     * @param file 업로드할 이미지 파일 (multipart/form-data)
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @return 업로드 결과 (신규 이미지 키 포함)
     */
    @PostMapping("/me/upload-image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String userEmail;
        try {
            userEmail = extractUserEmailFromToken(request);
        } catch (RuntimeException authException) {
            logger.warn("프로필 이미지 업로드 실패 - 인증 오류: {}", authException.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(authException.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            logger.info("프로필 이미지 업로드 요청 - 사용자: {}, 파일: {}, 크기: {}",
                    userEmail,
                    file != null ? file.getOriginalFilename() : "null",
                    file != null ? file.getSize() : 0);

            String imageKey = userTblService.updateProfileImage(userEmail, file);

            Map<String, Object> responseData = Map.of(
                "imageKey", imageKey,
                "hasImage", true
            );

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "프로필 이미지가 성공적으로 업로드되었습니다.", responseData);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException invalidRequest) {
            logger.warn("프로필 이미지 업로드 실패(잘못된 요청) - 사용자: {}, 오류: {}",
                    userEmail, invalidRequest.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(invalidRequest.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (ResourceNotFoundException notFound) {
            logger.warn("프로필 이미지 업로드 실패(사용자 없음) - 사용자: {}, 오류: {}",
                    userEmail, notFound.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(notFound.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (RuntimeException runtimeException) {
            logger.error("프로필 이미지 업로드 실패 - 사용자: {}, 오류: {}",
                    userEmail, runtimeException.getMessage(), runtimeException);
            String message = runtimeException.getMessage() != null
                ? runtimeException.getMessage()
                : "프로필 이미지 업로드 중 오류가 발생했습니다.";
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(message);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            logger.error("프로필 이미지 업로드 중 시스템 오류 - 사용자: {}, 오류: {}",
                    userEmail, e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(
                "프로필 이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출하고 사용자 이메일을 반환
     *
     * @param request HTTP 요청
     * @return JWT 토큰에서 추출한 사용자 이메일
     * @throws RuntimeException 토큰이 없거나 유효하지 않은 경우
     */
    private String extractUserEmailFromToken(HttpServletRequest request) {
        // Authorization 헤더에서 Bearer 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("인증 토큰이 필요합니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        // 토큰 유효성 검증
        if (!jwtUtil.isTokenValid(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        // 토큰에서 사용자 이메일 추출
        String userEmail = jwtUtil.extractUsername(token);
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new RuntimeException("토큰에서 사용자 정보를 찾을 수 없습니다.");
        }

        return userEmail;
    }

    /**
     * 프로필 이미지 URL 조회
     * 사용자의 프로필 이미지에 접근할 수 있는 Presigned URL을 반환
     *
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @return 프로필 이미지 URL 정보
     *
     * 요청 예시:
     * POST /api/profile/me/image
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "프로필 이미지 URL을 성공적으로 조회했습니다.",
     *   "data": {
     *     "imageUrl": "http://minio:9000/user-profiles/avatars/profile_123.jpg?X-Amz-Algorithm=...",
     *     "hasImage": true,
     *     "imageKey": "profile_123.jpg"
     *   },
     *   "timestamp": "2024-08-27T10:30:00Z"
     * }
     */
    @PostMapping("/me/image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfileImage(HttpServletRequest request) {
        try {
            // JWT 토큰에서 사용자 이메일 추출
            String userEmail = extractUserEmailFromToken(request);

            logger.info("프로필 이미지 조회 요청 - 사용자: {}", userEmail);

            // 프로필 정보 조회
            ProfileView profile = profileService.getMyProfile(userEmail);

            // 이미지 URL 생성
            Map<String, Object> imageData = createImageResponseData(profile.getProfileImageKey());

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "프로필 이미지 정보를 성공적으로 조회했습니다.", imageData);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("프로필 이미지 조회 실패: {}", e.getMessage());
            ApiResponse<Map<String, Object>> response = ApiResponse.failure(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            logger.error("프로필 이미지 조회 중 시스템 오류 발생: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = ApiResponse.failure("프로필 이미지 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 프로필 이미지 파일 직접 조회 (프록시 방식) - POST 방식
     * JWT 토큰으로 본인 인증 후 MinIO에서 이미지를 가져와 직접 전달
     *
     * @param imageRequest 이미지 요청 정보 (imageKey 포함)
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @return 이미지 파일 바이너리 데이터
     *
     * 요청 예시:
     * POST /api/profile/me/image/file
     * Content-Type: application/json
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * {
     *   "imageKey": "profile_123.jpg"
     * }
     *
     * 장점:
     * - MinIO 서버 직접 노출 없음
     * - JWT 기반 보안 (본인 이미지만 접근)
     * - POST 방식으로 보안 강화
     */
    @PostMapping("/me/image/file")
    public ResponseEntity<Resource> getMyProfileImageFile(
            @RequestBody ImageRequest imageRequest,
            HttpServletRequest request) {

        try {
            // JWT 토큰에서 사용자 이메일 추출
            String userEmail = extractUserEmailFromToken(request);
            
            // 요청에서 이미지 키 추출
            String imageKey = imageRequest.getImageKey();
            
            // 입력 유효성 검사
            if (imageKey == null || imageKey.trim().isEmpty()) {
                logger.warn("이미지 키가 제공되지 않음 - 사용자: {}", userEmail);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            logger.info("프로필 이미지 파일 조회 요청 - 사용자: {}, 이미지: {}", userEmail, imageKey);

            // 프로필 정보 조회로 본인 이미지인지 검증
            ProfileView profile = profileService.getMyProfile(userEmail);

            // 본인의 이미지만 접근 가능하도록 검증
            if (profile.getProfileImageKey() == null ||
                !imageKey.equals(profile.getProfileImageKey())) {
                logger.warn("권한 없는 이미지 접근 시도 - 사용자: {}, 요청이미지: {}, 실제이미지: {}",
                           userEmail, imageKey, profile.getProfileImageKey());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 캐시된 이미지 또는 MinIO에서 이미지 조회
            byte[] imageBytes = getCachedOrFetchImage(imageKey);

            // Content-Type 설정
            String contentType = minIOService.guessContentType(imageKey);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setCacheControl("public, max-age=86400"); // 24시간 브라우저 캐시
            headers.setContentLength(imageBytes.length);
            headers.add("Content-Disposition", "inline; filename=\"" + imageKey + "\"");

            // ByteArrayResource로 변환 (캐시된 바이트 배열 사용)
            Resource resource = new org.springframework.core.io.ByteArrayResource(imageBytes);

            logger.info("프로필 이미지 파일 전송 성공 - 사용자: {}, 이미지: {}", userEmail, imageKey);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            String imageKey = imageRequest != null ? imageRequest.getImageKey() : "unknown";
            logger.warn("잘못된 이미지 요청 - 이미지: {}, 오류: {}", imageKey, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (RuntimeException e) {
            String imageKey = imageRequest != null ? imageRequest.getImageKey() : "unknown";
            logger.warn("프로필 이미지 접근 실패 - 이미지: {}, 오류: {}", imageKey, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            String imageKey = imageRequest != null ? imageRequest.getImageKey() : "unknown";
            logger.error("프로필 이미지 조회 중 시스템 오류 - 이미지: {}, 오류: {}", imageKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 우편번호를 5자리 문자열로 포맷팅 (앞자리 0 패딩)
     * DB에 Integer로 저장된 우편번호를 문자열로 변환 시 사용
     *
     * @param zipCode 우편번호 (Integer, 예: 5852)
     * @return 5자리 문자열 (예: "05852")
     *
     * 예시:
     * - formatPostalCode(5852) → "05852"
     * - formatPostalCode(13529) → "13529"
     * - formatPostalCode(null) → ""
     */
    private String formatPostalCode(Integer zipCode) {
        if (zipCode == null) {
            return "";
        }
        // %05d: 5자리로 포맷하고, 부족한 자리는 0으로 채움
        return String.format("%05d", zipCode);
    }

    /**
     * 프로필 정보와 이미지 URL을 포함한 응답 데이터 생성
     *
     * @param profile 프로필 뷰 정보
     * @return 이미지 URL이 포함된 프로필 데이터
     */
    private Map<String, Object> createProfileResponseWithImage(ProfileView profile) {
        // HashMap을 사용하여 가변 맵 생성 (Map.of()는 20개 항목 제한 및 불변)
        Map<String, Object> profileData = new java.util.HashMap<>();

        // 기본 사용자 정보
        profileData.put("userEmail", profile.getUserEmail() != null ? profile.getUserEmail() : "");
        profileData.put("userName", profile.getUserName() != null ? profile.getUserName() : "");
        profileData.put("userPhone", profile.getUserPhone() != null ? profile.getUserPhone() : "");
        profileData.put("userType", profile.getUserType() != null ? profile.getUserType() : 0);
        profileData.put("userTypeText", profile.getUserTypeText() != null ? profile.getUserTypeText() : "");
        profileData.put("majorCode", profile.getMajorCode() != null ? profile.getMajorCode() : "");

        // 주소 정보 (우편번호는 0 패딩 처리)
        String zipCodeStr = profile.getZipCode();
        if (zipCodeStr != null && !zipCodeStr.trim().isEmpty()) {
            try {
                Integer zipCodeInt = Integer.parseInt(zipCodeStr);
                profileData.put("zipCode", formatPostalCode(zipCodeInt));
            } catch (NumberFormatException e) {
                profileData.put("zipCode", zipCodeStr); // 변환 실패 시 원본 사용
            }
        } else {
            profileData.put("zipCode", "");
        }
        profileData.put("mainAddress", profile.getMainAddress() != null ? profile.getMainAddress() : "");
        profileData.put("detailAddress", profile.getDetailAddress() != null ? profile.getDetailAddress() : "");
        
        // 개인 정보
        profileData.put("birthDate", profile.getBirthDate() != null ? profile.getBirthDate() : "");
        profileData.put("academicStatus", profile.getAcademicStatus() != null ? profile.getAcademicStatus() : "");
        profileData.put("admissionRoute", profile.getAdmissionRoute() != null ? profile.getAdmissionRoute() : "");
        
        // 전공 정보
        profileData.put("majorFacultyCode", profile.getMajorFacultyCode() != null ? profile.getMajorFacultyCode() : "");
        profileData.put("majorDeptCode", profile.getMajorDeptCode() != null ? profile.getMajorDeptCode() : "");
        profileData.put("minorFacultyCode", profile.getMinorFacultyCode() != null ? profile.getMinorFacultyCode() : "");
        profileData.put("minorDeptCode", profile.getMinorDeptCode() != null ? profile.getMinorDeptCode() : "");
        
        // 전공/부전공 정보 존재 여부
        profileData.put("hasMajorInfo", profile.hasMajorInfo());
        profileData.put("hasMinorInfo", profile.hasMinorInfo());
        
        // 프로필 이미지 정보
        profileData.put("image", createImageResponseData(profile.getProfileImageKey()));

        return profileData;
    }

    /**
     * 이미지 응답 데이터 생성 (간소화된 버전)
     * 이미지 존재 여부와 키만 제공, URL은 프론트엔드에서 생성
     *
     * @param imageKey 프로필 이미지 키
     * @return 이미지 기본 정보 (hasImage, imageKey)
     */
    private Map<String, Object> createImageResponseData(String imageKey) {
        boolean hasImage = imageKey != null && !imageKey.trim().isEmpty();

        if (hasImage) {
            try {
                // MinIO 연결 상태 확인 (이미지가 실제로 존재하는지 체크)
                boolean imageExists = minIOService.imageExists(imageKey);
                if (!imageExists) {
                    hasImage = false;
                    logger.warn("MinIO에서 이미지를 찾을 수 없음 - Key: {}", imageKey);
                }

            } catch (Exception e) {
                logger.warn("이미지 존재 확인 중 오류 발생 - Key: {}, Error: {}", imageKey, e.getMessage());
                hasImage = false;
            }
        }

        return Map.of(
            "hasImage", hasImage,
            "imageKey", imageKey != null ? imageKey : ""
        );
    }

    /**
     * 캐시된 이미지를 조회하거나 MinIO에서 가져와서 캐싱
     * 원본 이미지만 처리하는 단순화된 헬퍼 메서드
     *
     * @param imageKey 이미지 키
     * @return 이미지 바이트 배열
     * @throws Exception MinIO 연결 오류 또는 파일 없음
     */
    private byte[] getCachedOrFetchImage(String imageKey) throws Exception {
        try {
            // MinIO에서 원본 이미지 스트림 조회
            InputStream imageStream = minIOService.getProfileImageStream(imageKey);

            // 캐시 서비스를 통해 이미지 바이트 배열 조회/생성
            return imageCacheService.getCachedImageBytes(imageKey, imageStream);

        } catch (Exception e) {
            logger.error("이미지 조회 실패 - Key: {}, Error: {}", imageKey, e.getMessage());
            throw e;
        }
    }
}
