// 작성자: AI Assistant
// 학생별 수강 가능 강의 조회 API 테스트 (0값 규칙 적용)

/* ========================================
 * API 테스트: 학생별 수강 가능 강의 조회
 * ========================================
 *
 * 엔드포인트: GET /api/lectures/eligible/{studentId}
 * 기능: 0값 제한없음 규칙을 적용한 수강 가능 강의 필터링
 * 
 * 주요 검증 항목:
 * 1. 학생 권한 확인 (USER_STUDENT = 0)
 * 2. 강의 개설 여부 (LEC_OPEN = 1)
 * 3. 정원 확인 (LEC_CURRENT < LEC_MANY)
 * 4. 0값 규칙 적용 (향후 확장 예정)
 *    - LEC_MCODE = "0" → 모든 학부 수강 가능
 *    - LEC_MCODE_DEP = "0" → 모든 학과 수강 가능  
 *    - LEC_MIN = 0 → 모든 학년 수강 가능
 */

console.clear();
console.log("🎓 학생별 수강 가능 강의 조회 API 테스트 시작");
console.log("=" .repeat(60));

// API 기본 정보
const API_BASE_URL = "/api/lectures";
const TEST_STUDENT_ID = 1; // 테스트용 학생 ID

// ========== 테스트 함수들 ==========

/**
 * 1. 기본 수강 가능 강의 조회 테스트
 */
async function testBasicEligibleLectures() {
    console.log("\n📋 1. 기본 수강 가능 강의 조회 테스트");
    console.log("-".repeat(50));
    
    try {
        const response = await fetch(`${API_BASE_URL}/eligible/${TEST_STUDENT_ID}`);
        const data = await response.json();
        
        if (response.ok) {
            console.log("✅ 요청 성공!");
            console.log("📊 통계 정보:");
            console.log(`   - 전체 강의 수: ${data.totalCount}`);
            console.log(`   - 수강 가능: ${data.eligibleCount}`);
            console.log(`   - 수강 불가: ${data.ineligibleCount}`);
            
            console.log("\n📚 수강 가능 강의 목록:");
            data.eligibleLectures.forEach((lecture, index) => {
                console.log(`   ${index + 1}. ${lecture.lecTit} (${lecture.lecSerial})`);
                console.log(`      - 교수: ${lecture.lecProf}`);
                console.log(`      - 정원: ${lecture.lecCurrent}/${lecture.lecMany}`);
                console.log(`      - 자격: ${lecture.eligibilityReason}`);
                console.log(`      - 학부: ${lecture.lecMcode}, 학과: ${lecture.lecMcodeDep}, 최소학년: ${lecture.lecMin}`);
            });
            
            console.log("\n👤 학생 정보:");
            console.log(`   - ID: ${data.studentInfo.userIdx}`);
            console.log(`   - 이름: ${data.studentInfo.userName}`);
            console.log(`   - 이메일: ${data.studentInfo.userEmail}`);
            console.log(`   - 권한: ${data.studentInfo.userStudent === 0 ? '학생' : '교수'}`);
            
        } else {
            console.log("❌ 요청 실패!");
            console.log("응답:", data);
        }
        
        return data;
        
    } catch (error) {
        console.error("❌ 에러 발생:", error);
        return null;
    }
}

/**
 * 2. 페이징 테스트
 */
async function testPagination() {
    console.log("\n📄 2. 페이징 테스트");
    console.log("-".repeat(50));
    
    try {
        // 첫 번째 페이지 (크기: 5)
        const response1 = await fetch(`${API_BASE_URL}/eligible/${TEST_STUDENT_ID}?page=0&size=5`);
        const data1 = await response1.json();
        
        if (response1.ok) {
            console.log("✅ 첫 번째 페이지 조회 성공!");
            console.log(`📊 페이징 정보: ${data1.pagination.currentPage + 1}/${data1.pagination.totalPages}`);
            console.log(`📚 강의 수: ${data1.eligibleLectures.length}개`);
            
            // 두 번째 페이지가 있다면 조회
            if (data1.pagination.totalPages > 1) {
                const response2 = await fetch(`${API_BASE_URL}/eligible/${TEST_STUDENT_ID}?page=1&size=5`);
                const data2 = await response2.json();
                
                if (response2.ok) {
                    console.log("✅ 두 번째 페이지 조회 성공!");
                    console.log(`📊 페이징 정보: ${data2.pagination.currentPage + 1}/${data2.pagination.totalPages}`);
                    console.log(`📚 강의 수: ${data2.eligibleLectures.length}개`);
                }
            }
        } else {
            console.log("❌ 페이징 테스트 실패!");
            console.log("응답:", data1);
        }
        
    } catch (error) {
        console.error("❌ 페이징 테스트 에러:", error);
    }
}

/**
 * 3. 잘못된 학생 ID 테스트
 */
async function testInvalidStudentId() {
    console.log("\n🚫 3. 잘못된 학생 ID 테스트");
    console.log("-".repeat(50));
    
    const invalidIds = [999999, -1, "abc"];
    
    for (const invalidId of invalidIds) {
        try {
            const response = await fetch(`${API_BASE_URL}/eligible/${invalidId}`);
            const data = await response.json();
            
            console.log(`📝 ID ${invalidId} 테스트:`);
            if (!response.ok) {
                console.log("✅ 예상대로 실패!");
                console.log(`   오류 메시지: ${data.message || '알 수 없는 오류'}`);
            } else {
                console.log("⚠️ 예상과 다르게 성공함");
                console.log("   응답:", data);
            }
            
        } catch (error) {
            console.log(`❌ ID ${invalidId} 요청 중 에러:`, error);
        }
    }
}

/**
 * 4. 교수 권한 테스트 (학생이 아닌 경우)
 */
async function testProfessorPermission() {
    console.log("\n👨‍🏫 4. 교수 권한 테스트");
    console.log("-".repeat(50));
    
    // 교수 권한을 가진 사용자 ID (USER_STUDENT = 1)
    const professorId = 2; // 실제 교수 ID로 변경 필요
    
    try {
        const response = await fetch(`${API_BASE_URL}/eligible/${professorId}`);
        const data = await response.json();
        
        if (!response.ok) {
            console.log("✅ 예상대로 권한 오류!");
            console.log(`   오류 메시지: ${data.message || '권한 부족'}`);
        } else {
            console.log("⚠️ 예상과 다르게 성공함 (교수도 조회 가능?)");
            console.log("   응답:", data);
        }
        
    } catch (error) {
        console.error("❌ 교수 권한 테스트 에러:", error);
    }
}

/**
 * 5. 0값 규칙 확인 테스트
 */
async function testZeroValueRules() {
    console.log("\n⚠️ 5. 0값 규칙 확인 테스트");
    console.log("-".repeat(50));
    
    try {
        const response = await fetch(`${API_BASE_URL}/eligible/${TEST_STUDENT_ID}`);
        const data = await response.json();
        
        if (response.ok && data.eligibleLectures) {
            console.log("📊 0값 규칙 적용 현황 분석:");
            
            let zeroFacultyCount = 0;
            let zeroDeptCount = 0;
            let zeroMinGradeCount = 0;
            
            data.eligibleLectures.forEach(lecture => {
                if (lecture.lecMcode === "0") zeroFacultyCount++;
                if (lecture.lecMcodeDep === "0") zeroDeptCount++;
                if (lecture.lecMin === 0) zeroMinGradeCount++;
            });
            
            console.log(`   - 학부 제한 없음 (학부코드=0): ${zeroFacultyCount}개 강의`);
            console.log(`   - 학과 제한 없음 (학과코드=0): ${zeroDeptCount}개 강의`);
            console.log(`   - 학년 제한 없음 (최소학년=0): ${zeroMinGradeCount}개 강의`);
            
            // 0값 규칙 예시 출력
            console.log("\n📝 0값 규칙 예시:");
            data.eligibleLectures.slice(0, 3).forEach((lecture, index) => {
                console.log(`   ${index + 1}. ${lecture.lecTit}`);
                console.log(`      - 학부: ${lecture.lecMcode === "0" ? "제한없음" : lecture.lecMcode}`);
                console.log(`      - 학과: ${lecture.lecMcodeDep === "0" ? "제한없음" : lecture.lecMcodeDep}`);
                console.log(`      - 최소학년: ${lecture.lecMin === 0 ? "제한없음" : lecture.lecMin + "학년"}`);
            });
            
        } else {
            console.log("❌ 0값 규칙 테스트 실패!");
        }
        
    } catch (error) {
        console.error("❌ 0값 규칙 테스트 에러:", error);
    }
}

// ========== 테스트 실행 ==========

/**
 * 모든 테스트 실행
 */
async function runAllTests() {
    console.log("🚀 모든 테스트 시작...\n");
    
    await testBasicEligibleLectures();
    await testPagination();
    await testInvalidStudentId();
    await testProfessorPermission();
    await testZeroValueRules();
    
    console.log("\n" + "=".repeat(60));
    console.log("✅ 모든 테스트 완료!");
    console.log("\n💡 참고사항:");
    console.log("- 현재는 기본적인 개설여부/정원 검증만 구현됨");
    console.log("- 학생의 학부/학과/학년 정보가 UserTbl에 없어 0값 규칙 완전 구현 대기중");
    console.log("- 향후 UserTbl 확장 시 완전한 0값 규칙 적용 예정");
}

// 개별 테스트 실행 함수들도 글로벌로 노출
window.testEligibleLectures = {
    runAll: runAllTests,
    basic: testBasicEligibleLectures,
    pagination: testPagination,
    invalidId: testInvalidStudentId,
    professor: testProfessorPermission,
    zeroRules: testZeroValueRules
};

// 자동 실행
runAllTests();