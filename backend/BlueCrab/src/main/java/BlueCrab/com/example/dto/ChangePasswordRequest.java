package BlueCrab.com.example.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class ChangePasswordRequest {
    
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8-20자이며, 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String newPassword;
}