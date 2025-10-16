package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VanChuyenModel {
    private Integer maVanChuyen;
    
    @NotNull(message = "Người giao hàng không được để trống")
    private Integer maNguoiGiaoHang;
    
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer maDatHang;
    
    @NotBlank(message = "Trạng thái vận chuyển không được để trống")
    private String trangThai;
    
    // For response
    private String tenNguoiGiaoHang;
    private String sdtNguoiGiaoHang;
    private String tenNguoiDung;
}