package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "DanhGia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DanhGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhGia")
    private Integer maDanhGia;
    
    @NotNull(message = "Sản phẩm không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaSanPham", nullable = false)
    @JsonIgnoreProperties({"danhGias", "gioHangs", "chiTietDatHangs"})
    private SanPham sanPham;
    
    @NotNull(message = "Người dùng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiDung", nullable = false)
    @JsonIgnoreProperties({"danhGias", "cuaHangs", "gioHangs", "datHangs", "matKhau"})
    private NguoiDung nguoiDung;
    
    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao phải từ 1 đến 5")
    @Max(value = 5, message = "Số sao phải từ 1 đến 5")
    @Column(name = "SoSao")
    private Integer soSao;
    
    @Column(name = "BinhLuan", columnDefinition = "NVARCHAR(MAX)")
    @Builder.Default
    private String binhLuan = "Không có bình luận"; 
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "NgayDanhGia")
    @Builder.Default
    private Date ngayDanhGia = new Date();
    
    @Column(name = "Anh_Video", columnDefinition = "NVARCHAR(255)")
    private String anhVideo;
}