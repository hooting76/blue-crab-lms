package BlueCrab.com.example.config;

import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserTblRepository userTblRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 기존 관리자 계정이 없는 경우에만 생성
        if (userTblRepository.findByUserEmail("admin@bluecrab.com").isEmpty()) {
            UserTbl admin = new UserTbl();
            admin.setUserEmail("admin@bluecrab.com");
            admin.setUserName("시스템 관리자");
            admin.setUserPw(passwordEncoder.encode("admin123!"));
            admin.setUserPhone("01012345678");
            admin.setUserBirth("19900101");
            admin.setUserStudent(0); // 교수
            admin.setUserReg("20250825");
            admin.setUserRegIp("127.0.0.1");
            userTblRepository.save(admin);
            
            System.out.println("✅ 관리자 계정 생성 완료: admin@bluecrab.com / admin123!");
        }

        // 테스트 사용자 계정 생성
        if (userTblRepository.findByUserEmail("test@bluecrab.com").isEmpty()) {
            UserTbl testUser = new UserTbl();
            testUser.setUserEmail("test@bluecrab.com");
            testUser.setUserName("테스트 사용자");
            testUser.setUserPw(passwordEncoder.encode("test123!"));
            testUser.setUserPhone("01087654321");
            testUser.setUserBirth("19950315");
            testUser.setUserStudent(1); // 학생
            testUser.setUserReg("20250825");
            testUser.setUserRegIp("127.0.0.1");
            userTblRepository.save(testUser);
            
            System.out.println("✅ 테스트 계정 생성 완료: test@bluecrab.com / test123!");
        }

        System.out.println("🚀 개발용 시드 데이터 로딩 완료!");
    }
}
