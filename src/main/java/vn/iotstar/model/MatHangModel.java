package vn.iotstar.model;

import lombok.*;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatHangModel {
    private Integer maMatHang;
    
    @NotNull(message = "Giỏ hàng không được để trống")
    private Integer maGioHang;
    
    @NotNull(message = "Sản phẩm không được để trống")
    private Integer maSanPham;
    
    @NotNull(message = "Số lượng đặt không được để trống")
    @Min(value = 1, message = "Số lượng đặt phải lớn hơn 0")
    private Integer soLuongDat;
    

    private String tenSanPham;
    private BigDecimal giaBan;
    private String hinhAnh;
    private BigDecimal thanhTien;
}