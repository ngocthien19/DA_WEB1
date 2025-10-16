package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Entity
@Table(name = "NguoiDung")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaNguoiDung")
    private Integer maNguoiDung;
    
    @NotBlank(message = "Tên người dùng không được để trống")
    @Size(min = 2, max = 100, message = "Tên người dùng phải từ 2 đến 100 ký tự")
    @Column(name = "TenNguoiDung", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String tenNguoiDung;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Column(name = "Email", nullable = false, unique = true, columnDefinition = "VARCHAR(100)")
    private String email;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Column(name = "MatKhau", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String matKhau;
    
    @Pattern(regexp = "^(|0[0-9]{9})$", message = "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số")
    @Column(name = "SDT", columnDefinition = "VARCHAR(20)")
    private String sdt;
    
    @Column(name = "DiaChi", columnDefinition = "NVARCHAR(255)")
    private String diaChi;
    
    @Column(name = "HinhAnh", columnDefinition = "NVARCHAR(500)")
    private String hinhAnh;
    
    @NotNull(message = "Vai trò không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaVaiTro", nullable = false)
    private VaiTro vaiTro;
    
    @Column(name = "TrangThai", columnDefinition = "NVARCHAR(20)")
    @Builder.Default
    private String trangThai = "Hoạt động";
    
    @Column(name = "LyDoKhoa", columnDefinition = "NVARCHAR(500)")
    private String lyDoKhoa;
    
    @OneToMany(mappedBy = "nguoiDung", cascade = CascadeType.ALL)
    private List<CuaHang> cuaHangs;
    
    @OneToMany(mappedBy = "nguoiDung", cascade = CascadeType.ALL)
    private List<GioHang> gioHangs;
    
    @OneToMany(mappedBy = "nguoiDung", cascade = CascadeType.ALL)
    private List<DatHang> datHangs;
    
    @OneToMany(mappedBy = "nguoiDung", cascade = CascadeType.ALL)
    private List<DanhGia> danhGias;
    
    @OneToMany(mappedBy = "nguoiDung", cascade = CascadeType.ALL)
    private List<VanChuyen> vanChuyens; // Thêm quan hệ mới
    
    // Phương thức kiểm tra xem người dùng có phải là shipper không
    public boolean isShipper() {
        return vaiTro != null && "SHIPPER".equalsIgnoreCase(vaiTro.getTenVaiTro());
    }
}