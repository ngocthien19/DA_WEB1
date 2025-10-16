package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaoDonHangModel {
    @NotNull(message = "Người dùng không được để trống")
    private Integer maNguoiDung;
    
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String phuongThucThanhToan;
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String diaChiGiaoHang;
    
    @NotNull(message = "Danh sách sản phẩm không được để trống")
    @Size(min = 1, message = "Phải có ít nhất 1 sản phẩm")
    private List<SanPhamDonHangModel> sanPham;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SanPhamDonHangModel {
    @NotNull(message = "Mã sản phẩm không được để trống")
    private Integer maSanPham;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;
    
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán không được âm")
    private BigDecimal giaBan;
}