package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "MatHang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaMatHang")
    private Integer maMatHang;
    
    @NotNull(message = "Giỏ hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaGioHang", nullable = false)
    private GioHang gioHang;
    
    @NotNull(message = "Sản phẩm không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;
    
    @NotNull(message = "Số lượng đặt không được để trống")
    @Min(value = 1, message = "Số lượng đặt phải lớn hơn 0")
    @Column(name = "SoLuongDat", nullable = false)
    private Integer soLuongDat;
}