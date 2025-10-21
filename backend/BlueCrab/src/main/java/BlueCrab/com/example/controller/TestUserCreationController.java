package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.UserCreationRequestDTO;
import BlueCrab.com.example.dto.UserCreationTestResponse;
import BlueCrab.com.example.entity.RegistryTbl;
import BlueCrab.com.example.entity.SerialCodeTable;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.RegistryRepository;
import BlueCrab.com.example.repository.SerialCodeTableRepository;
import BlueCrab.com.example.service.UserTblService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 테스트 환경에서 사용자 및 연관 데이터를 한 번에 생성하기 위한 컨트롤러.
 * 학적/전공 레코드가 정상적으로 저장되는지 검증하는 용도다.
 */
@RestController
@RequestMapping("/api/test/users")
public class TestUserCreationController {

    @Autowired
    private UserTblService userTblService;

    @Autowired
    private SerialCodeTableRepository serialCodeTableRepository;

    @Autowired
    private RegistryRepository registryRepository;

    @PostMapping("/bootstrap")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserCreationTestResponse>> bootstrapUser(
        @Valid @RequestBody UserCreationRequestDTO request
    ) {
        UserTbl createdUser = userTblService.createUser(request);

        SerialCodeTable serialCode = serialCodeTableRepository
            .findByUserIdx(createdUser.getUserIdx())
            .orElse(null);

        RegistryTbl registry = registryRepository
            .findLatestByUserIdx(createdUser.getUserIdx())
            .orElse(null);

        UserCreationTestResponse payload = UserCreationTestResponse.from(
            createdUser,
            serialCode,
            registry
        );

        return ResponseEntity.ok(
            ApiResponse.success("테스트 사용자와 연관 데이터가 생성되었습니다.", payload)
        );
    }
}
