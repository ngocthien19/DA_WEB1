package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "SanPham")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaSanPham")
    private Integer maSanPham;
    
    @NotNull(message = "Cửa hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaCuaHang", nullable = false)
    private CuaHang cuaHang;
    
    @NotNull(message = "Danh mục không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaDanhMuc", nullable = false)
    private DanhMuc danhMuc;
    
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 2, max = 255, message = "Tên sản phẩm phải từ 2 đến 255 ký tự")
    @Column(name = "TenSanPham", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tenSanPham;
    
    @Size(max = 2000, message = "Mô tả sản phẩm không được quá 2000 ký tự")
    @Column(name = "MoTaSanPham", columnDefinition = "NVARCHAR(MAX)")
    private String moTaSanPham;
    
    @NotNull(message = "Lượt thích không được để trống")
    @DecimalMin(value = "0", message = "Lượt thích không được âm")
    @Column(name = "LuotThich", precision = 18, scale = 0)
    @Builder.Default
    private BigDecimal luotThich = BigDecimal.ZERO;
    
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    @Column(name = "GiaBan", precision = 18, scale = 2)
    private BigDecimal giaBan;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    @Column(name = "SoLuongConLai")
    @Builder.Default
    private Integer soLuongConLai = 0;
    
    @NotNull(message = "Số lượng đã bán không được để trống")
    @Min(value = 0, message = "Số lượng đã bán không được âm")
    @Column(name = "SoLuongDaBan")
    @Builder.Default
    private Integer soLuongDaBan = 0;
    
    @Temporal(TemporalType.DATE)
    @Column(name = "NgayNhap")
    private Date ngayNhap;
    
    @Column(name = "HinhAnh", columnDefinition = "NVARCHAR(255)")
    private String hinhAnh;
    
    @NotBlank(message = "Loại sản phẩm không được để trống")
    @Column(name = "LoaiSanPham", columnDefinition = "NVARCHAR(255)")
    private String loaiSanPham;
    
    @Column(name = "TrangThai")
    @Builder.Default
    private Boolean trangThai = true;
    
    @Column(name = "SaoDanhGia")
    private Integer saoDanhGia;
    
    @OneToOne(mappedBy = "sanPham", cascade = CascadeType.ALL)
    private ThuCung thuCung;
    
    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL)
    private List<MatHang> matHangs;
    
    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL)
    private List<DatHangChiTiet> datHangChiTiets;
    
    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL)
    private List<DanhGia> danhGias;
}