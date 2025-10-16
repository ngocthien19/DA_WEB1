package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "DatHang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDatHang")
    private Integer maDatHang;
    
    @NotNull(message = "Người dùng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiDung", nullable = false)
    private NguoiDung nguoiDung;
    
    @Column(name = "NgayDat")
    @Builder.Default
    private LocalDate ngayDat = LocalDate.now();
    
    @NotNull(message = "Tổng tiền không được để trống")
    @DecimalMin(value = "0.0", message = "Tổng tiền không được âm")
    @Column(name = "TongTien", precision = 18, scale = 2)
    private BigDecimal tongTien;
    
    @NotBlank(message = "Trạng thái không được để trống")
    @Column(name = "TrangThai", columnDefinition = "NVARCHAR(50)")
    @Builder.Default
    private String trangThai = "New";
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(max = 500, message = "Địa chỉ giao hàng không được quá 500 ký tự")
    @Column(name = "DiaChiGiaoHang", columnDefinition = "NVARCHAR(500)")
    private String diaChiGiaoHang;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "(84|0[3|5|7|8|9])+([0-9]{8})\\b", message = "Số điện thoại không hợp lệ")
    @Size(max = 20, message = "Số điện thoại không được quá 20 ký tự")
    @Column(name = "SoDienThoaiGiaoHang", columnDefinition = "VARCHAR(20)")
    private String soDienThoaiGiaoHang;

    @Size(max = 1000, message = "Ghi chú không được quá 1000 ký tự")
    @Column(name = "GhiChu", columnDefinition = "NVARCHAR(1000)")
    private String ghiChu;
    
    @Size(max = 500, message = "Lý do hủy không được quá 500 ký tự")
    @Column(name = "LyDoHuy", columnDefinition = "NVARCHAR(500)")
    private String lyDoHuy;

    // Thêm quan hệ với KhuyenMai (nhiều-1)
    @ManyToOne
    @JoinColumn(name = "MaKhuyenMai")
    private KhuyenMai khuyenMai;
    
 // Thêm vào entity DatHang
    @ManyToOne
    @JoinColumn(name = "MaShipper")
    private NguoiDung shipper;
    
    @OneToMany(mappedBy = "datHang", cascade = CascadeType.ALL)
    private List<DatHangChiTiet> datHangChiTiets;
    
    @OneToMany(mappedBy = "datHang", cascade = CascadeType.ALL)
    private List<VanChuyen> vanChuyens;
    
    @OneToMany(mappedBy = "datHang", cascade = CascadeType.ALL)
    private List<ThanhToan> thanhToans;
}