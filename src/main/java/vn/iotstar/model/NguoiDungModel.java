package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDungModel {
    
    // Validation groups
    public interface Create {}
    public interface Update {}
    
    private Integer maNguoiDung;
    
    @NotBlank(message = "Tên người dùng không được để trống", groups = {Create.class, Update.class})
    @Size(min = 2, max = 100, message = "Tên người dùng phải từ 2 đến 100 ký tự", groups = {Create.class, Update.class})
    private String tenNguoiDung;
    
    @NotBlank(message = "Email không được để trống", groups = {Create.class, Update.class})
    @Email(message = "Email không đúng định dạng", groups = {Create.class, Update.class})
    private String email;
    
    @NotBlank(message = "Mật khẩu không được để trống", groups = {Create.class})
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự", groups = {Create.class})
    private String matKhau;
    
    @Pattern(regexp = "^(|0[0-9]{9})$", message = "Số điện thoại phải bắt đầu bằng 0 và có 10 chữ số", groups = {Create.class, Update.class})
    private String sdt;
    
    private String diaChi;
    
    private String hinhAnh;
    
    @NotBlank(message = "Vai trò không được để trống", groups = {Create.class, Update.class})
    private String maVaiTro;
    
    private String trangThai;
    
    // For response
    private String tenVaiTro;
}