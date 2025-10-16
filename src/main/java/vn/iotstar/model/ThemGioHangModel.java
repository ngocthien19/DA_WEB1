package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemGioHangModel {
    @NotNull(message = "Mã người dùng không được để trống")
    private Integer maNguoiDung;
    
    @NotNull(message = "Mã sản phẩm không được để trống")
    private Integer maSanPham;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;
}