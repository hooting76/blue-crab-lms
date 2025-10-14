package BlueCrab.com.example.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

/**
 * 랜덤 주소 생성 유틸리티
 * 
 * 신규 사용자 등록 시 임의의 주소를 자동 생성
 * 실제 주소 형식을 따르지만 임의로 생성된 가상 주소
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-14
 */
@Component
public class RandomAddressGenerator {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private static final String[] CITIES = {
        "서울특별시", "부산광역시", "인천광역시", "대구광역시",
        "대전광역시", "광주광역시", "울산광역시", "세종특별자치시",
        "경기도", "강원도", "충청북도", "충청남도",
        "전라북도", "전라남도", "경상북도", "경상남도", "제주특별자치도"
    };
    
    private static final Map<String, String[]> DISTRICTS = Map.ofEntries(
        Map.entry("서울특별시", new String[]{"강남구", "서초구", "송파구", "강동구", "중구", "용산구", "성동구", "광진구", "동대문구", "중랑구"}),
        Map.entry("부산광역시", new String[]{"해운대구", "수영구", "남구", "동래구", "연제구", "부산진구", "사하구", "북구"}),
        Map.entry("인천광역시", new String[]{"남동구", "연수구", "부평구", "계양구", "서구", "중구", "동구", "미추홀구"}),
        Map.entry("대구광역시", new String[]{"중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군"}),
        Map.entry("대전광역시", new String[]{"동구", "중구", "서구", "유성구", "대덕구"}),
        Map.entry("광주광역시", new String[]{"동구", "서구", "남구", "북구", "광산구"}),
        Map.entry("울산광역시", new String[]{"중구", "남구", "동구", "북구", "울주군"}),
        Map.entry("세종특별자치시", new String[]{"조치원읍", "연기면", "전동면", "소정면"}),
        Map.entry("경기도", new String[]{"수원시", "성남시", "고양시", "용인시", "부천시", "안산시", "안양시", "남양주시", "화성시", "평택시"}),
        Map.entry("강원도", new String[]{"춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시"}),
        Map.entry("충청북도", new String[]{"청주시", "충주시", "제천시", "보은군", "옥천군", "영동군"}),
        Map.entry("충청남도", new String[]{"천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시"}),
        Map.entry("전라북도", new String[]{"전주시", "군산시", "익산시", "정읍시", "남원시", "김제시"}),
        Map.entry("전라남도", new String[]{"목포시", "여수시", "순천시", "나주시", "광양시"}),
        Map.entry("경상북도", new String[]{"포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시"}),
        Map.entry("경상남도", new String[]{"창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시"}),
        Map.entry("제주특별자치도", new String[]{"제주시", "서귀포시"})
    );
    
    private static final String[] STREET_NAMES = {
        "중앙로", "시청로", "역삼로", "테헤란로", "강남대로", "서초대로", "송파대로",
        "해운대로", "광안로", "수영로", "남천로", "벡스코로",
        "인천로", "송도대로", "청라대로", "구월로",
        "동성로", "달구벌대로", "수성로", "범어로",
        "둔산로", "대덕대로", "유성대로", "도안대로",
        "충장로", "금남로", "상무대로", "수완로",
        "삼산로", "번영로", "남부순환로", "태화로",
        "한빛대로", "조치원로", "새롬로", "달빛로"
    };
    
    private static final String[] BUILDING_TYPES = {
        "아파트", "빌라", "오피스텔", "주택", "타운하우스"
    };
    
    /**
     * 랜덤 주소 정보 생성
     * 
     * @return AddressInfo 객체 (우편번호, 기본주소, 상세주소)
     */
    public AddressInfo generateRandomAddress() {
        String city = CITIES[RANDOM.nextInt(CITIES.length)];
        String[] cityDistricts = DISTRICTS.getOrDefault(city, new String[]{"중구"});
        String district = cityDistricts[RANDOM.nextInt(cityDistricts.length)];
        
        String streetName = STREET_NAMES[RANDOM.nextInt(STREET_NAMES.length)];
        int streetNumber = RANDOM.nextInt(500) + 1;
        
        String mainAddress = String.format("%s %s %s %d", city, district, streetName, streetNumber);
        
        String detailAddress = generateDetailAddress();
        
        String zipCode = generateZipCode();
        
        return new AddressInfo(zipCode, mainAddress, detailAddress);
    }
    
    /**
     * 우편번호 생성 (5자리)
     * 실제 우편번호 형식을 따름 (지역별 범위 고려)
     */
    private String generateZipCode() {
        // 실제 우편번호는 01000 ~ 63999 범위
        int zipNum = RANDOM.nextInt(63000) + 1000;
        return String.format("%05d", zipNum);
    }
    
    /**
     * 상세 주소 생성 (건물명, 동, 호수)
     */
    private String generateDetailAddress() {
        String buildingType = BUILDING_TYPES[RANDOM.nextInt(BUILDING_TYPES.length)];
        
        if (buildingType.equals("아파트") || buildingType.equals("오피스텔")) {
            int dong = RANDOM.nextInt(20) + 1;
            int ho = RANDOM.nextInt(10) + 1;
            int number = RANDOM.nextInt(20) + 1;
            return String.format("%d동 %d%02d호", dong, ho, number);
        } else if (buildingType.equals("빌라")) {
            int floor = RANDOM.nextInt(5) + 1;
            int ho = RANDOM.nextInt(4) + 1;
            return String.format("%d층 %d호", floor, ho);
        } else {
            return null; // 단독주택은 상세주소 없음
        }
    }
    
    /**
     * 간단한 주소만 생성 (상세주소 없음)
     * 
     * @return AddressInfo 객체 (detailAddress는 null)
     */
    public AddressInfo generateSimpleAddress() {
        String city = CITIES[RANDOM.nextInt(CITIES.length)];
        String[] cityDistricts = DISTRICTS.getOrDefault(city, new String[]{"중구"});
        String district = cityDistricts[RANDOM.nextInt(cityDistricts.length)];
        
        String streetName = STREET_NAMES[RANDOM.nextInt(STREET_NAMES.length)];
        int streetNumber = RANDOM.nextInt(500) + 1;
        
        String mainAddress = String.format("%s %s %s %d", city, district, streetName, streetNumber);
        String zipCode = generateZipCode();
        
        return new AddressInfo(zipCode, mainAddress, null);
    }
    
    /**
     * 주소 정보를 담는 내부 클래스
     */
    public static class AddressInfo {
        private final String zipCode;
        private final String mainAddress;
        private final String detailAddress;
        
        public AddressInfo(String zipCode, String mainAddress, String detailAddress) {
            this.zipCode = zipCode;
            this.mainAddress = mainAddress;
            this.detailAddress = detailAddress;
        }
        
        public String getZipCode() {
            return zipCode;
        }
        
        public String getMainAddress() {
            return mainAddress;
        }
        
        public String getDetailAddress() {
            return detailAddress;
        }
        
        public String getFullAddress() {
            if (detailAddress != null && !detailAddress.isEmpty()) {
                return mainAddress + ", " + detailAddress;
            }
            return mainAddress;
        }
        
        @Override
        public String toString() {
            return String.format("AddressInfo{zipCode='%s', mainAddress='%s', detailAddress='%s'}",
                zipCode, mainAddress, detailAddress);
        }
    }
}
