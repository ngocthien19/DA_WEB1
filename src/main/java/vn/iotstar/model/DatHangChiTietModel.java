package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatHangChiTietModel {
    private Integer maDatHangChiTiet;
    
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer maDatHang;
    
    @NotNull(message = "Sản phẩm không được để trống")
    private Integer maSanPham;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;
    
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán không được âm")
    private BigDecimal giaBan;
    
    @NotNull(message = "Thành tiền không được để trống")
    @DecimalMin(value = "0.0", message = "Thành tiền không được âm")
    private BigDecimal thanhTien;
    
    // For response
    private String tenSanPham;
    private String hinhAnh;
}