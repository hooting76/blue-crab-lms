package BlueCrab.com.example.controller;

import BlueCrab.com.example.entity.Lecture.Department;
import BlueCrab.com.example.entity.Lecture.Faculty;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.repository.Lecture.DepartmentRepository;
import BlueCrab.com.example.repository.Lecture.FacultyRepository;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 필터 옵션 데이터 제공 컨트롤러
 *
 * 프론트엔드가 필터 UI를 표시하는 데 필요한 드롭다운 옵션 제공
 */
@RestController
@RequestMapping("/api/admin/filter-options")
@PreAuthorize("hasRole('ADMIN')")
public class FilterOptionsController {

    @Autowired
    private LecTblRepository lecTblRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * 강좌 목록 조회 (강좌별 필터용)
     *
     * POST /api/admin/filter-options/courses
     *
     * Request Body: {} (empty body)
     *
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "name": "데이터베이스",
     *     "code": "CS301"
     *   },
     *   {
     *     "id": 2,
     *     "name": "알고리즘",
     *     "code": "CS302"
     *   }
     * ]
     */
    @PostMapping("/courses")
    public ResponseEntity<List<Map<String, Object>>> getCourses() {
        List<LecTbl> lectures = lecTblRepository.findAll();

        List<Map<String, Object>> courses = lectures.stream()
            .map(lecture -> {
                Map<String, Object> course = new HashMap<>();
                course.put("id", lecture.getLecIdx());
                course.put("name", lecture.getLecTit());
                course.put("code", lecture.getLecSerial());
                return course;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }

    /**
     * 역할 옵션 조회 (역할별 필터용)
     *
     * POST /api/admin/filter-options/roles
     *
     * Request Body: {} (empty body)
     *
     * Response:
     * [
     *   {
     *     "value": 0,
     *     "label": "교수"
     *   },
     *   {
     *     "value": 1,
     *     "label": "학생"
     *   }
     * ]
     */
    @PostMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getRoles() {
        List<Map<String, Object>> roles = List.of(
            Map.of("value", 0, "label", "교수"),
            Map.of("value", 1, "label", "학생")
        );

        return ResponseEntity.ok(roles);
    }

    /**
     * 학부 옵션 조회 (학부별 필터용)
     *
     * POST /api/admin/filter-options/faculties
     *
     * Response:
     * [
     *   {"id":1,"code":"01","name":"공과대학"},
     *   {"id":2,"code":"02","name":"인문대학"}
     * ]
     */
    @PostMapping("/faculties")
    public ResponseEntity<List<Map<String, Object>>> getFaculties() {
        List<Faculty> faculties = facultyRepository.findAll();

        List<Map<String, Object>> response = faculties.stream()
                .map(faculty -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", faculty.getFacultyId());
                    item.put("code", faculty.getFacultyCode());
                    item.put("name", faculty.getFacultyName());
                    item.put("establishedAt", faculty.getEstablishedAt());
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 학과 옵션 조회 (학과별 필터용)
     *
     * POST /api/admin/filter-options/departments
     *
     * Request Body (선택):
     * {
     *   "facultyCode": "01"
     * }
     */
    @PostMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getDepartments(
            @RequestBody(required = false) Map<String, String> request) {

        String facultyCode = request != null ? request.get("facultyCode") : null;

        List<Department> departments;
        Map<Integer, Faculty> facultyLookup;

        if (facultyCode != null && !facultyCode.trim().isEmpty()) {
            Optional<Faculty> facultyOpt = facultyRepository.findByFacultyCode(facultyCode.trim());
            if (facultyOpt.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            Faculty faculty = facultyOpt.get();
            departments = departmentRepository.findByFacultyId(faculty.getFacultyId());
            facultyLookup = Map.of(faculty.getFacultyId(), faculty);
        } else {
            departments = departmentRepository.findAll();
            facultyLookup = facultyRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(
                            Faculty::getFacultyId,
                            Function.identity()
                    ));
        }

        List<Map<String, Object>> response = departments.stream()
                .map(department -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", department.getDeptId());
                    item.put("code", department.getDeptCode());
                    item.put("name", department.getDeptName());
                    item.put("facultyId", department.getFacultyId());

                    Faculty faculty = facultyLookup.get(department.getFacultyId());
                    if (faculty != null) {
                        item.put("facultyCode", faculty.getFacultyCode());
                        item.put("facultyName", faculty.getFacultyName());
                    }

                    item.put("establishedAt", department.getEstablishedAt());
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

}
