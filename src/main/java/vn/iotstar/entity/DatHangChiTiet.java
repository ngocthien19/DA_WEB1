package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "DatHangChiTiet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatHangChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDatHangChiTiet")
    private Integer maDatHangChiTiet;
    
    @NotNull(message = "Đơn hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaDatHang", nullable = false)
    private DatHang datHang;
    
    @NotNull(message = "Sản phẩm không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;
    
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán không được âm")
    @Column(name = "GiaBan", precision = 18, scale = 2)
    private BigDecimal giaBan;
    
    @NotNull(message = "Thành tiền không được để trống")
    @DecimalMin(value = "0.0", message = "Thành tiền không được âm")
    @Column(name = "ThanhTien", precision = 18, scale = 2)
    private BigDecimal thanhTien;
}