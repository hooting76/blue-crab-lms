package BlueCrab.com.example.dto.Lecture.Attendance;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 출석 API 공통 응답 DTO
 * POST /api/attendance/request, PUT /api/attendance/approve
 * 
 * 성공/실패 여부와 메시지, 선택적으로 데이터를 반환
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceResponseDto<T> {

    private Boolean success;                                // 성공 여부
    private String message;                                 // 응답 메시지
    private T data;                                         // 응답 데이터 (선택사항)

    // Constructors

    public AttendanceResponseDto() {}

    public AttendanceResponseDto(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AttendanceResponseDto(Boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Static Factory Methods

    public static <T> AttendanceResponseDto<T> success(String message) {
        return new AttendanceResponseDto<>(true, message);
    }

    public static <T> AttendanceResponseDto<T> success(String message, T data) {
        return new AttendanceResponseDto<>(true, message, data);
    }

    public static <T> AttendanceResponseDto<T> error(String message) {
        return new AttendanceResponseDto<>(false, message);
    }

    public static <T> AttendanceResponseDto<T> error(String message, T data) {
        return new AttendanceResponseDto<>(false, message, data);
    }

    // Getters and Setters

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "AttendanceResponseDto{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
