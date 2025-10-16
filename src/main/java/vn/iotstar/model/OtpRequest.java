package vn.iotstar.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    
    @NotBlank(message = "Tên người dùng không được để trống")
    @Size(min = 2, max = 100, message = "Tên người dùng phải từ 2 đến 100 ký tự")
    private String tenNguoiDung;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String matKhau;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có 10 số")
    private String sdt;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 5, max = 255, message = "Địa chỉ phải từ 5 đến 255 ký tự")
    private String diaChi;
}