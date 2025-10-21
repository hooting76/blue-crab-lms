package BlueCrab.com.example.dto;

import BlueCrab.com.example.entity.RegistryTbl;
import BlueCrab.com.example.entity.SerialCodeTable;
import BlueCrab.com.example.entity.UserTbl;

/**
 * 테스트용 사용자 생성 결과 응답 DTO.
 * 연관 테이블에 기록된 핵심 필드를 요약하여 반환한다.
 */
public class UserCreationTestResponse {

    private final UserSummary user;
    private final SerialCodeSummary serialCode;
    private final RegistrySummary registry;

    private UserCreationTestResponse(
        UserSummary user,
        SerialCodeSummary serialCode,
        RegistrySummary registry
    ) {
        this.user = user;
        this.serialCode = serialCode;
        this.registry = registry;
    }

    public static UserCreationTestResponse from(
        UserTbl user,
        SerialCodeTable serialCodeEntity,
        RegistryTbl registryEntity
    ) {
        return new UserCreationTestResponse(
            UserSummary.from(user),
            SerialCodeSummary.from(serialCodeEntity),
            RegistrySummary.from(registryEntity)
        );
    }

    public UserSummary getUser() {
        return user;
    }

    public SerialCodeSummary getSerialCode() {
        return serialCode;
    }

    public RegistrySummary getRegistry() {
        return registry;
    }

    public static class UserSummary {
        private final Integer userIdx;
        private final String userEmail;
        private final String userName;
        private final String userCode;
        private final Integer userStudent;
        private final String userBirth;
        private final String userReg;
        private final String userRegIp;

        private UserSummary(
            Integer userIdx,
            String userEmail,
            String userName,
            String userCode,
            Integer userStudent,
            String userBirth,
            String userReg,
            String userRegIp
        ) {
            this.userIdx = userIdx;
            this.userEmail = userEmail;
            this.userName = userName;
            this.userCode = userCode;
            this.userStudent = userStudent;
            this.userBirth = userBirth;
            this.userReg = userReg;
            this.userRegIp = userRegIp;
        }

        public static UserSummary from(UserTbl user) {
            if (user == null) {
                return null;
            }
            return new UserSummary(
                user.getUserIdx(),
                user.getUserEmail(),
                user.getUserName(),
                user.getUserCode(),
                user.getUserStudent(),
                user.getUserBirth(),
                user.getUserReg(),
                user.getUserRegIp()
            );
        }

        public Integer getUserIdx() {
            return userIdx;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserCode() {
            return userCode;
        }

        public Integer getUserStudent() {
            return userStudent;
        }

        public String getUserBirth() {
            return userBirth;
        }

        public String getUserReg() {
            return userReg;
        }

        public String getUserRegIp() {
            return userRegIp;
        }
    }

    public static class SerialCodeSummary {
        private final Integer serialIdx;
        private final String serialCode;
        private final String serialSub;
        private final String serialCodeNd;
        private final String serialSubNd;
        private final String serialReg;
        private final String serialRegNd;

        private SerialCodeSummary(
            Integer serialIdx,
            String serialCode,
            String serialSub,
            String serialCodeNd,
            String serialSubNd,
            String serialReg,
            String serialRegNd
        ) {
            this.serialIdx = serialIdx;
            this.serialCode = serialCode;
            this.serialSub = serialSub;
            this.serialCodeNd = serialCodeNd;
            this.serialSubNd = serialSubNd;
            this.serialReg = serialReg;
            this.serialRegNd = serialRegNd;
        }

        public static SerialCodeSummary from(SerialCodeTable serialCode) {
            if (serialCode == null) {
                return null;
            }
            return new SerialCodeSummary(
                serialCode.getSerialIdx(),
                serialCode.getSerialCode(),
                serialCode.getSerialSub(),
                serialCode.getSerialCodeNd(),
                serialCode.getSerialSubNd(),
                serialCode.getSerialReg(),
                serialCode.getSerialRegNd()
            );
        }

        public Integer getSerialIdx() {
            return serialIdx;
        }

        public String getSerialCode() {
            return serialCode;
        }

        public String getSerialSub() {
            return serialSub;
        }

        public String getSerialCodeNd() {
            return serialCodeNd;
        }

        public String getSerialSubNd() {
            return serialSubNd;
        }

        public String getSerialReg() {
            return serialReg;
        }

        public String getSerialRegNd() {
            return serialRegNd;
        }
    }

    public static class RegistrySummary {
        private final Integer regIdx;
        private final String joinPath;
        private final String stdStat;
        private final Integer cntTerm;
        private final String stdRestDate;
        private final String adminName;
        private final String adminReg;
        private final String adminIp;

        private RegistrySummary(
            Integer regIdx,
            String joinPath,
            String stdStat,
            Integer cntTerm,
            String stdRestDate,
            String adminName,
            String adminReg,
            String adminIp
        ) {
            this.regIdx = regIdx;
            this.joinPath = joinPath;
            this.stdStat = stdStat;
            this.cntTerm = cntTerm;
            this.stdRestDate = stdRestDate;
            this.adminName = adminName;
            this.adminReg = adminReg;
            this.adminIp = adminIp;
        }

        public static RegistrySummary from(RegistryTbl registry) {
            if (registry == null) {
                return null;
            }
            return new RegistrySummary(
                registry.getRegIdx(),
                registry.getJoinPath(),
                registry.getStdStat(),
                registry.getCntTerm(),
                registry.getStdRestDate(),
                registry.getAdminName(),
                registry.getAdminReg() != null ? registry.getAdminReg().toString() : null,
                registry.getAdminIp()
            );
        }

        public Integer getRegIdx() {
            return regIdx;
        }

        public String getJoinPath() {
            return joinPath;
        }

        public String getStdStat() {
            return stdStat;
        }

        public Integer getCntTerm() {
            return cntTerm;
        }

        public String getStdRestDate() {
            return stdRestDate;
        }

        public String getAdminName() {
            return adminName;
        }

        public String getAdminReg() {
            return adminReg;
        }

        public String getAdminIp() {
            return adminIp;
        }
    }
}
