package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "ThanhToan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaThanhToan")
    private Integer maThanhToan;
    
    @NotNull(message = "Đơn hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaDatHang", nullable = false)
    private DatHang datHang;
    
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Column(name = "PhuongThuc", columnDefinition = "NVARCHAR(50)")
    private String phuongThuc;
    
    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền thanh toán không được âm")
    @Column(name = "SoTienThanhToan", precision = 18, scale = 2)
    private BigDecimal soTienThanhToan;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "NgayThanhToan")
    private Date ngayThanhToan;
    
    @NotBlank(message = "Trạng thái thanh toán không được để trống")
    @Column(name = "TrangThai", columnDefinition = "NVARCHAR(50)")
    @Builder.Default
    private String trangThai = "Pending";
}