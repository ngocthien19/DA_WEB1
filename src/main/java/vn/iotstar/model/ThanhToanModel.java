package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThanhToanModel {
    private Integer maThanhToan;
    
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer maDatHang;
    
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String phuongThuc;
    
    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền thanh toán không được âm")
    private BigDecimal soTienThanhToan;
    
    private Date ngayThanhToan;
    
    @NotBlank(message = "Trạng thái thanh toán không được để trống")
    private String trangThai;
    
    // For response
    private String tenNguoiDung;
    private BigDecimal tongTienDonHang;
}