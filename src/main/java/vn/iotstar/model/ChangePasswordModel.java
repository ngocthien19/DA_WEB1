package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordModel {
    
    @NotNull(message = "Mã người dùng không được để trống")
    private Integer maNguoiDung;
    
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String matKhauCu;
    
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String matKhauMoi;
    
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String xacNhanMatKhau;
}