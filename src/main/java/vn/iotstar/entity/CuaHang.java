package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "CuaHang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuaHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaCuaHang")
    private Integer maCuaHang;
    
    @NotNull(message = "Chủ cửa hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiDung", nullable = false)
    private NguoiDung nguoiDung;
    
    @NotBlank(message = "Tên cửa hàng không được để trống")
    @Size(min = 2, max = 100, message = "Tên cửa hàng phải từ 2 đến 100 ký tự")
    @Column(name = "TenCuaHang", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String tenCuaHang;
    
    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    @Column(name = "MoTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "NgayTao")
    @Builder.Default
    private Date ngayTao = new Date();
    
    @Column(name = "HinhAnh", columnDefinition = "NVARCHAR(255)")
    private String hinhAnh;
    
    // Các trường mới thêm vào với validate
    @NotBlank(message = "Địa chỉ cửa hàng không được để trống")
    @Size(max = 255, message = "Địa chỉ không được quá 255 ký tự")
    @Column(name = "DiaChi", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String diaChi;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "(84|0[3|5|7|8|9])+([0-9]{8})\\b", message = "Số điện thoại không hợp lệ")
    @Size(max = 20, message = "Số điện thoại không được quá 20 ký tự")
    @Column(name = "SoDienThoai", nullable = false, columnDefinition = "VARCHAR(20)")
    private String soDienThoai;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được quá 100 ký tự")
    @Column(name = "Email", nullable = false, columnDefinition = "VARCHAR(100)")
    private String email;
    
    @Min(value = 0, message = "Đánh giá trung bình không được nhỏ hơn 0")
    @Max(value = 5, message = "Đánh giá trung bình không được lớn hơn 5")
    @Column(name = "DanhGiaTrungBinh")
    private Double danhGiaTrungBinh;
    
    @Min(value = 0, message = "Số lượng đánh giá không được nhỏ hơn 0")
    @Column(name = "SoLuongDanhGia")
    @Builder.Default
    private Integer soLuongDanhGia = 0;
    
    @Min(value = 1900, message = "Năm thành lập phải từ 1900")
    @Max(value = 2100, message = "Năm thành lập không được vượt quá 2100")
    @Column(name = "NamThanhLap")
    private Integer namThanhLap;
     
    @NotNull(message = "Trạng thái không được để trống")
    @Column(name = "TrangThai", nullable = false)
    @Builder.Default
    private Boolean trangThai = true;
    
    @Column(name = "LyDoKhoa", columnDefinition = "NVARCHAR(500)")
    private String lyDoKhoa;
    
    @OneToMany(mappedBy = "cuaHang", cascade = CascadeType.ALL)
    private List<SanPham> sanPhams;
    
    @OneToMany(mappedBy = "cuaHang", cascade = CascadeType.ALL)
    private List<KhuyenMai> khuyenMais;
    
    // Custom validation methods
    @AssertTrue(message = "Năm thành lập không được lớn hơn năm hiện tại")
    public boolean isNamThanhLapValid() {
        if (namThanhLap == null) return true;
        int currentYear = java.time.Year.now().getValue();
        return namThanhLap <= currentYear;
    }
    
    @AssertTrue(message = "Đánh giá trung bình phải có số lượng đánh giá tương ứng")
    public boolean isDanhGiaValid() {
        // Nếu chưa có đánh giá nào (cửa hàng mới) - luôn hợp lệ
        if ((danhGiaTrungBinh == null || danhGiaTrungBinh == 0.0) && 
            (soLuongDanhGia == null || soLuongDanhGia == 0)) {
            return true;
        }
        
        // Nếu có đánh giá trung bình thì phải có số lượng đánh giá tương ứng
        if (danhGiaTrungBinh != null && danhGiaTrungBinh > 0.0 && 
            soLuongDanhGia != null && soLuongDanhGia > 0) {
            return true;
        }
        
        // Các trường hợp khác không hợp lệ
        return false;
    }
}