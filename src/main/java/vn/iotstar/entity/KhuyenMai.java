package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "KhuyenMai")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhuyenMai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKhuyenMai")
    private Integer maKhuyenMai;
    
    @NotNull(message = "Cửa hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaCuaHang", nullable = false)
    private CuaHang cuaHang;
    
    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(min = 3, max = 50, message = "Mã giảm giá phải từ 3 đến 50 ký tự")
    @Column(name = "MaGiamGia", nullable = false, columnDefinition = "VARCHAR(50)")
    private String maGiamGia;
    
    @NotNull(message = "Discount không được để trống")
    @DecimalMin(value = "0.0", message = "Discount không được âm")
    @DecimalMax(value = "100.0", message = "Discount không được vượt quá 100")
    @Column(name = "Discount", nullable = false, precision = 5, scale = 2)
    private BigDecimal discount;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(name = "NgayBatDau", nullable = false)
    private LocalDate ngayBatDau;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    @Column(name = "NgayKetThuc", nullable = false)
    private LocalDate ngayKetThuc; 
    
    @NotNull(message = "Số lượng mã giảm giá không được để trống")
    @Min(value = 0, message = "Số lượng mã giảm giá không được âm")
    @Column(name = "SoLuongMaGiamGia")
    private Integer soLuongMaGiamGia;
    
    @Column(name = "SoLuongDaSuDung", columnDefinition = "int default 0")
    @Builder.Default
    private Integer soLuongDaSuDung = 0;
    
    // ========== THÊM TRƯỜNG TRẠNG THÁI ==========
    @NotNull(message = "Trạng thái không được để trống")
    @Column(name = "TrangThai", nullable = false, columnDefinition = "bit default 1")
    @Builder.Default
    private Boolean trangThai = true;
    
    // ========== METHODS ĐỂ TÍNH TRẠNG THÁI HIỆN TẠI ==========
    @Transient
    public String getTrangThaiHienTai() {
        LocalDate now = LocalDate.now();
        if (!trangThai) {
            return "Ngừng kích hoạt";
        }
        if (now.isBefore(ngayBatDau)) {
            return "Sắp diễn ra";
        }
        if (now.isAfter(ngayKetThuc)) {
            return "Đã hết hạn";
        }
        return "Đang kích hoạt";
    }
    
    @Transient
    public boolean isActive() {
        return "Đang kích hoạt".equals(getTrangThaiHienTai());
    }
    
    @Transient
    public String getCssClass() {
        String status = getTrangThaiHienTai();
        switch (status) {
            case "Đang kích hoạt":
                return "status-active";
            case "Sắp diễn ra":
                return "status-upcoming";
            case "Đã hết hạn":
                return "status-expired";
            case "Ngừng kích hoạt":
                return "status-inactive";
            default:
                return "status-inactive";
        }
    }
}