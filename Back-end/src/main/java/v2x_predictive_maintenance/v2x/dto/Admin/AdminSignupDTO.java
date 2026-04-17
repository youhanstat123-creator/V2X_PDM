package v2x_predictive_maintenance.v2x.dto.Admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AdminSignupDTO {

    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,12}$",
            message = "아이디는 영어와 숫자 4~12자리입니다.")
    private String loginId; // 아이디

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하로 입력해주세요.")
    private String password; // 비밀번호

    @NotBlank(message = "이름을 입력해주세요.")
    @Pattern(regexp = "^[가-힣]{2,10}$",
            message = "이름은 한글만 입력 가능합니다.")
    private String adminName; // 이름

    @NotBlank(message = "사원번호는 필수입니다.")
    @Pattern(
            regexp = "^VTX\\d{3}$",
            message = "사원번호는 VTX001 형식으로 입력해주세요."
    )
    private String employeeNo; // 사원번호

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(
            regexp = "^010-\\d{4}-\\d{4}$",
            message = "휴대폰번호는 010-1234-5678 형식으로 입력해주세요."
    )
    private String phoneNumber; // 휴대번호
}