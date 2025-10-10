package BlueCrab.com.example.initializer;

import BlueCrab.com.example.entity.FacilityTbl;
import BlueCrab.com.example.enums.FacilityType;
import BlueCrab.com.example.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 시설 초기 데이터 생성을 위한 Initializer
 * dev 프로필에서만 실행
 */
@Component
@Profile("dev")
public class FacilityDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FacilityDataInitializer.class);
    private final FacilityRepository facilityRepository;

    public FacilityDataInitializer(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Override
    public void run(String... args) {
        if (facilityRepository.count() > 0) {
            logger.info("Facility data already exists. Skipping initialization.");
            return;
        }

        logger.info("Initializing facility data...");

        FacilityTbl seminar1 = new FacilityTbl();
        seminar1.setFacilityName("세미나실 1");
        seminar1.setFacilityTypeEnum(FacilityType.SEMINAR_ROOM);
        seminar1.setFacilityDesc("중형 세미나실로 프레젠테이션 장비 완비");
        seminar1.setCapacity(30);
        seminar1.setLocation("본관 3층");
        seminar1.setDefaultEquipment("빔프로젝터, 화이트보드, 마이크");
        seminar1.setIsActive(true);

        FacilityTbl lecture1 = new FacilityTbl();
        lecture1.setFacilityName("강의실 A101");
        lecture1.setFacilityTypeEnum(FacilityType.LECTURE_ROOM);
        lecture1.setFacilityDesc("대형 강의실");
        lecture1.setCapacity(100);
        lecture1.setLocation("A동 1층");
        lecture1.setDefaultEquipment("빔프로젝터, 마이크, 스피커");
        lecture1.setIsActive(true);

        FacilityTbl conference1 = new FacilityTbl();
        conference1.setFacilityName("회의실 201");
        conference1.setFacilityTypeEnum(FacilityType.CONFERENCE_ROOM);
        conference1.setFacilityDesc("소형 회의실");
        conference1.setCapacity(10);
        conference1.setLocation("본관 2층");
        conference1.setDefaultEquipment("화상회의 시스템, TV");
        conference1.setIsActive(true);

        facilityRepository.save(seminar1);
        facilityRepository.save(lecture1);
        facilityRepository.save(conference1);

        logger.info("Facility data initialization completed.");
    }
}
