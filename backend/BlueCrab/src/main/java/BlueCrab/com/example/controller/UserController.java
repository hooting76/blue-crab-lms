package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.UserCreationRequestDTO;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.service.UserTblService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;

/**
 * 사용자 관리를 위한 REST API 컨트롤러
 * UserTbl 엔티티 기반으로 사용자 CRUD 및 검색, 통계 기능을 제공
 *
 * ⚠️ 현재 상태 유지 및 추후 리팩토링 예정
 *
 * 현재 제공 기능:
 * - 전체 사용자 조회 (보안 검토 필요)
 * - 학생/교수별 조회
 * - 개별 사용자 조회
 * - 사용자 생성/수정/삭제
 * - 역할 변경
 * - 이름/키워드/생년월일 검색
 * - 사용자 통계 조회
 *
 * 🔄 추후 개발 계획:
 * - 요구사항 명세에 따라 기능별 분리 예정
 * - 보안 강화 (권한 체크, 데이터 필터링)
 * - 코드 중복 제거 및 공통 로직 추출
 * - 검색 기능 통합 및 최적화
 * - 페이징 및 캐싱 적용
 *
 * 📋 팀장과 상의 후 결정될 사항:
 * - 유지할 기능 vs 제거할 기능 분류
 * - 일반 사용자용 vs 관리자용 컨트롤러 분리 여부
 * - 개인정보 보호 수준 및 필터링 정책
 * - 검색 기능 범위 및 제한사항
 *
 * 모든 응답은 ApiResponse<T> 형식으로 통일되어 반환됩니다:
 * - success: 요청 성공/실패 여부 (boolean)
 * - message: 사용자에게 표시할 메시지 (한국어)
 * - data: 실제 응답 데이터 (UserTbl, List<UserTbl> 등)
 * - timestamp: 응답 생성 시간 (ISO-8601 형식)
 *
 * 예외 발생 시 GlobalExceptionHandler에서 일관된 형식으로 처리됩니다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserTblService userTblService;
    
    /**
     * 모든 사용자 조회
     * 
     * @return 전체 사용자 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserTbl>>> getAllUsers() {
        List<UserTbl> users = userTblService.getAllUsers();
        ApiResponse<List<UserTbl>> response = ApiResponse.success("사용자 목록을 성공적으로 조회했습니다.", users);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 학생 사용자만 조회
     * 
     * @return 학생 사용자 목록
     */
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<UserTbl>>> getStudentUsers() {
        List<UserTbl> users = userTblService.getStudentUsers();
        ApiResponse<List<UserTbl>> response = ApiResponse.success("학생 사용자 목록을 성공적으로 조회했습니다.", users);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 교수 사용자만 조회
     * 
     * @return 교수 사용자 목록
     */
    @GetMapping("/professors")
    public ResponseEntity<ApiResponse<List<UserTbl>>> getProfessorUsers() {
        List<UserTbl> users = userTblService.getProfessorUsers();
        ApiResponse<List<UserTbl>> response = ApiResponse.success("교수 사용자 목록을 성공적으로 조회했습니다.", users);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 사용자 조회
     * 
     * @param id 사용자 ID
     * @return 사용자 정보 또는 404 에러
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserTbl>> getUserById(@PathVariable Integer id) {
        Optional<UserTbl> user = userTblService.getUserById(id);
        if (user.isPresent()) {
            ApiResponse<UserTbl> response = ApiResponse.success("사용자를 성공적으로 조회했습니다.", user.get());
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserTbl> response = ApiResponse.failure("사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * 새 사용자 생성
     * 
     * @param user 생성할 사용자 정보
     * @return 생성된 사용자 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserTbl>> createUser(@Valid @RequestBody UserCreationRequestDTO request) {
        UserTbl createdUser = userTblService.createUser(request);
        ApiResponse<UserTbl> response = ApiResponse.success("사용자가 성공적으로 생성되었습니다.", createdUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 사용자 정보 수정
     * 
     * @param id 수정할 사용자 ID
     * @param userDetails 수정할 사용자 정보
     * @return 수정된 사용자 정보
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserTbl>> updateUser(@PathVariable Integer id, @RequestBody UserTbl userDetails) {
        UserTbl updatedUser = userTblService.updateUser(id, userDetails);
        ApiResponse<UserTbl> response = ApiResponse.success("사용자 정보가 성공적으로 수정되었습니다.", updatedUser);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 삭제
     * 
     * @param id 삭제할 사용자 ID
     * @return 삭제 결과 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteUser(@PathVariable Integer id) {
        userTblService.deleteUser(id);
        ApiResponse<Object> response = ApiResponse.success("사용자가 성공적으로 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 역할 전환 (학생 ↔ 교수)
     * 
     * @param id 역할을 변경할 사용자 ID
     * @return 역할이 변경된 사용자 정보
     */
    @PatchMapping("/{id}/toggle-role")
    public ResponseEntity<ApiResponse<UserTbl>> toggleUserRole(@PathVariable Integer id) {
        UserTbl user = userTblService.toggleUserRole(id);
        String roleMessage = user.getUserStudent() == 0 ? "학생" : "교수";
        ApiResponse<UserTbl> response = ApiResponse.success(
            String.format("사용자 역할이 '%s'로 변경되었습니다.", roleMessage), user);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 이름으로 사용자 검색
     * 
     * @param name 검색할 사용자 이름 (부분 매치)
     * @return 검색된 사용자 목록
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserTbl>>> searchUsers(@RequestParam String name) {
        List<UserTbl> users = userTblService.searchByName(name);
        String message = String.format("이름 '%s'로 검색된 사용자 %d명을 찾았습니다.", name, users.size());
        ApiResponse<List<UserTbl>> response = ApiResponse.success(message, users);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 키워드로 사용자 검색 (이름, 이메일 검색)
     * 
     * @param keyword 검색 키워드
     * @return 검색된 사용자 목록
     */
    @GetMapping("/search-all")
    public ResponseEntity<ApiResponse<List<UserTbl>>> searchAllUsers(@RequestParam String keyword) {
        List<UserTbl> users = userTblService.searchByKeyword(keyword);
        String message = String.format("키워드 '%s'로 검색된 사용자 %d명을 찾았습니다.", keyword, users.size());
        ApiResponse<List<UserTbl>> response = ApiResponse.success(message, users);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 생년월일 범위로 사용자 검색
     * 
     * @param startDate 시작 날짜 (YYYYMMDD)
     * @param endDate 종료 날짜 (YYYYMMDD)
     * @return 해당 범위의 사용자 목록
     */
    @GetMapping("/search-birth")
    public ResponseEntity<ApiResponse<List<UserTbl>>> searchUsersByBirth(
            @RequestParam String startDate, @RequestParam String endDate) {
        List<UserTbl> users = userTblService.searchByBirthRange(startDate, endDate);
        String message = String.format("생년월일 %s~%s 범위의 사용자 %d명을 찾았습니다.", startDate, endDate, users.size());
        ApiResponse<List<UserTbl>> response = ApiResponse.success(message, users);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 통계 정보 조회
     * 
     * @return 사용자 통계 정보 (전체, 학생, 교수 사용자 수 및 비율)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats() {
        Map<String, Object> stats = userTblService.getUserStats();
        ApiResponse<Map<String, Object>> response = ApiResponse.success("사용자 통계 정보를 성공적으로 조회했습니다.", stats);
        return ResponseEntity.ok(response);
    }
}
