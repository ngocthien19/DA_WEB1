package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatHangModel {
    private Integer maDatHang;
    
    @NotNull(message = "Người dùng không được để trống")
    private Integer maNguoiDung;
    
    private Date ngayDat;
    
    @NotNull(message = "Tổng tiền không được để trống")
    @DecimalMin(value = "0.0", message = "Tổng tiền không được âm")
    private BigDecimal tongTien;
    
    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;
    
    // For response
    private String tenNguoiDung;
    private String email;
    private String sdt;
    private String diaChi;
    private List<DatHangChiTietModel> chiTietDonHang;
}