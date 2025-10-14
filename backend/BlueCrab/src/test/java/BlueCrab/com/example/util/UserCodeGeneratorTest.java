package BlueCrab.com.example.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserCodeGenerator 테스트 클래스
 */
class UserCodeGeneratorTest {
    
    private UserCodeGenerator generator;
    
    @BeforeEach
    void setUp() {
        generator = new UserCodeGenerator();
    }
    
    @Test
    @DisplayName("학생 코드 생성 - 기본 형식 확인")
    void testGenerateStudentCode() {
        String code = generator.generateUserCode(
            2025, 
            UserCodeGenerator.UserType.STUDENT, 
            1, 
            5
        );
        
        // 형식 검증: YYYY-0BC-DE-FGH
        assertTrue(code.matches("\\d{4}-\\d{3}-\\d{2}-\\d{3}"), 
            "코드 형식이 올바르지 않습니다: " + code);
        
        // 내용 검증
        assertTrue(code.startsWith("2025-001-05-"), 
            "코드 시작 부분이 올바르지 않습니다: " + code);
        
        System.out.println("생성된 학생 코드: " + code);
    }
    
    @Test
    @DisplayName("교수 코드 생성 - 기본 형식 확인")
    void testGenerateProfessorCode() {
        String code = generator.generateUserCode(
            2024, 
            UserCodeGenerator.UserType.PROFESSOR, 
            10, 
            2
        );
        
        // 형식 검증
        assertTrue(code.matches("\\d{4}-\\d{3}-\\d{2}-\\d{3}"), 
            "코드 형식이 올바르지 않습니다: " + code);
        
        // 교수 구분값 확인 (1로 시작해야 함)
        assertTrue(code.startsWith("2024-110-02-"), 
            "교수 코드 시작 부분이 올바르지 않습니다: " + code);
        
        System.out.println("생성된 교수 코드: " + code);
    }
    
    @Test
    @DisplayName("미지정 학부/학과로 코드 생성")
    void testGenerateDefaultCode() {
        String code = generator.generateUserCodeDefault(
            2025, 
            UserCodeGenerator.UserType.STUDENT
        );
        
        assertTrue(code.startsWith("2025-000-00-"), 
            "기본 코드가 올바르지 않습니다: " + code);
        
        System.out.println("생성된 기본 학생 코드: " + code);
    }
    
    @Test
    @DisplayName("현재 연도로 코드 생성")
    void testGenerateCurrentYearCode() {
        String code = generator.generateUserCode(
            UserCodeGenerator.UserType.STUDENT, 
            3, 
            7
        );
        
        assertTrue(code.matches("\\d{4}-003-07-\\d{3}"), 
            "현재 연도 코드가 올바르지 않습니다: " + code);
        
        System.out.println("생성된 현재 연도 학생 코드: " + code);
    }
    
    @Test
    @DisplayName("코드 파싱 테스트")
    void testParseUserCode() {
        String code = "2025-001-05-847";
        
        UserCodeGenerator.UserCodeInfo info = UserCodeGenerator.parseUserCode(code);
        
        assertEquals(2025, info.getYear());
        assertEquals(UserCodeGenerator.UserType.STUDENT, info.getUserType());
        assertEquals(1, info.getFacultyCode());
        assertEquals(5, info.getDepartmentCode());
        assertEquals(847, info.getUniqueNumber());
        
        System.out.println("파싱된 정보: " + info);
    }
    
    @Test
    @DisplayName("교수 코드 파싱 테스트")
    void testParseProfessorCode() {
        String code = "2024-110-02-321";
        
        UserCodeGenerator.UserCodeInfo info = UserCodeGenerator.parseUserCode(code);
        
        assertEquals(2024, info.getYear());
        assertEquals(UserCodeGenerator.UserType.PROFESSOR, info.getUserType());
        assertEquals(10, info.getFacultyCode());
        assertEquals(2, info.getDepartmentCode());
        assertEquals(321, info.getUniqueNumber());
        
        System.out.println("파싱된 교수 정보: " + info);
    }
    
    @Test
    @DisplayName("고유 난수 범위 확인 (000~999)")
    void testUniqueNumberRange() {
        for (int i = 0; i < 10; i++) {
            String code = generator.generateUserCode(
                2025, 
                UserCodeGenerator.UserType.STUDENT, 
                0, 
                0
            );
            
            String uniquePart = code.substring(code.lastIndexOf("-") + 1);
            int uniqueNum = Integer.parseInt(uniquePart);
            
            assertTrue(uniqueNum >= 0 && uniqueNum <= 999, 
                "고유 번호가 범위를 벗어났습니다: " + uniqueNum);
        }
    }
    
    @Test
    @DisplayName("잘못된 년도 입력 - 예외 발생")
    void testInvalidYear() {
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateUserCode(
                1800, 
                UserCodeGenerator.UserType.STUDENT, 
                0, 
                0
            );
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateUserCode(
                10000, 
                UserCodeGenerator.UserType.STUDENT, 
                0, 
                0
            );
        });
    }
    
    @Test
    @DisplayName("잘못된 학부 코드 입력 - 예외 발생")
    void testInvalidFacultyCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateUserCode(
                2025, 
                UserCodeGenerator.UserType.STUDENT, 
                -1, 
                0
            );
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateUserCode(
                2025, 
                UserCodeGenerator.UserType.STUDENT, 
                100, 
                0
            );
        });
    }
    
    @Test
    @DisplayName("잘못된 학과 코드 입력 - 예외 발생")
    void testInvalidDepartmentCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateUserCode(
                2025, 
                UserCodeGenerator.UserType.STUDENT, 
                0, 
                -1
            );
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateUserCode(
                2025, 
                UserCodeGenerator.UserType.STUDENT, 
                0, 
                100
            );
        });
    }
    
    @Test
    @DisplayName("잘못된 코드 형식 파싱 - 예외 발생")
    void testInvalidCodeFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            UserCodeGenerator.parseUserCode("2025001-05-847");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            UserCodeGenerator.parseUserCode("invalid-code");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            UserCodeGenerator.parseUserCode(null);
        });
    }
    
    @Test
    @DisplayName("다수의 코드 생성 - 고유성 확인")
    void testMultipleCodeGeneration() {
        int count = 100;
        for (int i = 0; i < count; i++) {
            String studentCode = generator.generateUserCode(
                2025, 
                UserCodeGenerator.UserType.STUDENT, 
                i % 10, 
                i % 5
            );
            String profCode = generator.generateUserCode(
                2025, 
                UserCodeGenerator.UserType.PROFESSOR, 
                i % 10, 
                i % 5
            );
            
            assertNotNull(studentCode);
            assertNotNull(profCode);
            
            // 학생과 교수 코드는 달라야 함 (같은 학부/학과라도)
            if (i % 10 == 0 && i % 5 == 0) {
                // 같은 학부/학과지만 학생/교수 구분값이 다름
                assertNotEquals(
                    studentCode.substring(0, 6), 
                    profCode.substring(0, 6),
                    "학생과 교수 코드의 구분값이 달라야 합니다"
                );
            }
        }
        
        System.out.println(count + "개의 코드 생성 완료");
    }
}
