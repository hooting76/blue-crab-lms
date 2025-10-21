package BlueCrab.com.example.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "SERIAL_CODE_TABLE")
@Getter
@Setter
public class SerialCodeTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SERIAL_IDX")
    private Integer serialIdx;

    @Column(name = "USER_IDX", nullable = false)
    private Integer userIdx;

    @Column(name = "SERIAL_CODE", nullable = false, length = 2)
    private String serialCode; // 주전공 학부 코드

    @Column(name = "SERIAL_SUB", nullable = false, length = 2)
    private String serialSub; // 주전공 학과 코드

    @Column(name = "SERIAL_CODE_ND", length = 2)
    private String serialCodeNd; // 부전공 학부 코드

    @Column(name = "SERIAL_SUB_ND", length = 2)
    private String serialSubNd; // 부전공 학과 코드

    @Column(name = "SERIAL_REG", nullable = false, length = 50)
    private String serialReg; // 전공 등록일

    @Column(name = "SERIAL_REG_ND", length = 50)
    private String serialRegNd; // 부전공 등록일

    @PrePersist
    protected void onCreate() {
        this.serialReg = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
