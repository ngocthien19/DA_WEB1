package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiGiaoHangModel {
    private Integer maNguoiGiaoHang;
    
    @NotBlank(message = "Tên người giao hàng không được để trống")
    @Size(min = 2, max = 100, message = "Tên người giao hàng phải từ 2 đến 100 ký tự")
    private String tenNguoiGiaoHang;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Số điện thoại không đúng định dạng")
    private String sdtNguoiGiaoHang;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 10, max = 255, message = "Địa chỉ phải từ 10 đến 255 ký tự")
    private String diaChiNguoiGiaoHang;
}