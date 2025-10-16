package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhuyenMaiModel {
    private Integer maKhuyenMai;
    
    @NotNull(message = "Cửa hàng không được để trống")
    private Integer maCuaHang;
    
    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(min = 3, max = 50, message = "Mã giảm giá phải từ 3 đến 50 ký tự")
    private String maGiamGia;
    
    @NotNull(message = "Discount không được để trống")
    @DecimalMin(value = "0.0", message = "Discount không được âm")
    @DecimalMax(value = "100.0", message = "Discount không được vượt quá 100")
    private BigDecimal discount;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @FutureOrPresent(message = "Ngày bắt đầu phải là hiện tại hoặc tương lai")
    private LocalDate  ngayBatDau;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải là tương lai")
    private LocalDate  ngayKetThuc;
    
    @NotNull(message = "Số lượng mã giảm giá không được để trống")
    @Min(value = 0, message = "Số lượng mã giảm giá không được âm")
    private Integer soLuongMaGiamGia;
    
    private Integer soLuongDaSuDung; 
    
 // THÊM TRƯỜNG NÀY
    private Boolean trangThai;
    
    // For response
    private String tenCuaHang;
    private String tenChuCuaHang;
    private String trangThaiHienTai; // ĐANG KÍCH HOẠT, ĐÃ HẾT HẠN, SẮP DIỄN RA, NGỪNG KÍCH HOẠT
    private String cssClass;
}