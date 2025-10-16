package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "VanChuyen")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VanChuyen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaVanChuyen")
    private Integer maVanChuyen;
    
    @NotNull(message = "Người giao hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiDung", nullable = false) // Đổi tên cột thành MaNguoiDung
    private NguoiDung nguoiDung; // Đổi từ nguoiGiaoHang thành nguoiDung
    
    @NotNull(message = "Đơn hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaDatHang", nullable = false)
    private DatHang datHang;
    
    @NotBlank(message = "Trạng thái vận chuyển không được để trống")
    @Column(name = "TrangThai", columnDefinition = "NVARCHAR(20)")
    @Builder.Default
    private String trangThai = "Đã bàn giao";
    
    @Size(max = 500, message = "Lý do hủy không được quá 500 ký tự")
    @Column(name = "LyDoHuy", columnDefinition = "NVARCHAR(500)")
    private String lyDoHuy;
}